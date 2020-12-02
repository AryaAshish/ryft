'use strict';

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const md5 = require('md5')

const { Logging } = require('@google-cloud/logging');
const logging = new Logging();
const log = logging.log('my-custom-log-name');

admin.initializeApp();

const Sound = "default";
const iOSAction = "co.chatsdk.QuickReply";
const blockedUsersEnabled = true;
const loggingEnabled = true;
const reciprocalContactsEnabled = false;

const enableV4Compatibility = true;

const path = require('path');
const sharp = require('sharp');
const os = require('os');
const fs = require('fs');

exports.pushNotificationPostLikes = functions.database.ref('/post-likes/{postId}/{authorId}/{likeId}').onCreate((snap, context)  => {

    console.log('New post like was added');

    const likeAuthorId = context.params.authorId;
    const postId = context.params.postId;
    const likeId = context.params.likeId;

    return sendPostLikeNotification(postId,likeAuthorId,likeId);

});

exports.pushNotificationProjectLikes = functions.database.ref('/project-likes/{postId}/{authorId}/{likeId}').onCreate((snap, context)  => {

    console.log('New project like was added');

    const likeAuthorId = context.params.authorId;
    const postId = context.params.postId;
    const likeId = context.params.likeId;

    return sendProjectLikeNotification(postId,likeAuthorId,likeId);

});

exports.pushNotificationPostComments = functions.database.ref('/post-comments/{postId}/{commentId}').onCreate((snap, context) => {

    console.log('New post comment was added');

    const commentAuthorId = snap.val().authorId;
    const postId = context.params.postId;
    const commentId = context.params.commentId;

    return sendPostCommentNotification(postId,commentAuthorId,commentId);

});

exports.pushNotificationProjectComments = functions.database.ref('/project-comments/{postId}/{commentId}').onCreate((snap, context) => {

    console.log('New project comment was added');

    const commentAuthorId = snap.val().authorId;
    const postId = context.params.postId;
    const commentId = context.params.commentId;

    return sendProjectCommentNotification(postId,commentAuthorId,commentId);

});

exports.pushNotificationMessages = functions.database.ref('/messages/{toId}/{fromId}/{messageId}').onCreate((snap, context) => {

    const fromId = context.params.fromId;
    const toId = context.params.toId;
    const messageId = context.params.messageId;
    const from = snap.val().from;

    if(fromId === from){

        console.log("send a message notification");

        return sendMessageNotification(toId,fromId,messageId);

    }

    return null;

});

exports.pushNotificationGroupMessages = functions.database.ref('/groupchats/{projectId}/messages/{messageId}').onCreate((snap, context) => {

    const projectId = context.params.projectId;
    const messageId = context.params.messageId;
    const fromId = snap.val().from;

    return sendGroupMessageNotification(projectId,fromId,messageId);

});

exports.pushNotificationNewProjectApplication = functions.database.ref('/projects/{projectId}/responses/{responseId}').onCreate((snap, context) => {

    const projectId = context.params.projectId;
    const responseId = context.params.responseId;

    return sendNewProjectApplicationNotification(projectId,responseId);

});

exports.pushNotificationCandidateSelection = functions.database.ref('/projects/{projectId}/selectedCandidates/{candidateId}').onCreate((snap, context) => {

    const projectId = context.params.projectId;
    const candidateId = context.params.candidateId;

    return sendCandidateSelectionNotification(projectId,candidateId);

});

exports.syncUsersWithChatSdk = functions.https.onRequest(async (req,res) => {

    await sendTaskResult();

    res.json({result: `function fired.`});

});

async function sendTaskResult(){

    var date = new Date();
    var timeInMs = date.getTime();

    var db = admin.database();
    var ref = db.ref("profiles");

    var chatsdkref = db.ref("chatsdk/users");

    ref.once("value", function(snap) {

        console.log("profiles : ",snap.numChildren());
        
        let profiles = snap.val();
        let profilekeys = Object.keys(profiles);
        
        for(var i=0;i<profilekeys.length;i++){

            if(profiles[profilekeys[i]].username){

            var meta = new Object();

            meta['availability'] = 'available';
            if(profiles[profilekeys[i]].email){
                meta['email'] = profiles[profilekeys[i]].email;
            }
            else{
                meta['email'] = '';
            }
            meta['name'] = profiles[profilekeys[i]].username;
            meta['name-lowercase'] = profiles[profilekeys[i]].username.toLowerCase();
            meta['pictureURL'] = profiles[profilekeys[i]].photoUrl;

            chatsdkref.child(profilekeys[i]).child("last-online").set(timeInMs);
            chatsdkref.child(profilekeys[i]).child("meta").set(meta);

            }
            else{
                console.log("invalid profile:",profilekeys[i]);
            }

        }

    });

}

async function sendCandidateSelectionNotification(projectId,candidateId){
    
    const tokens = await getDeviceTokens(candidateId);

    if (tokens.length > 0) {

        // Notification details.

        // Create a notification
        let payload = {
            data: {
                actionType: actionTypeNewSelection,
                title: notificationTitle,
                body: `You are selected for a project`,
                postId: projectId
            },
        };

        console.log("send notification to : ",tokens[0]);
    
        // Send notifications to all tokens.
    
        return admin.messaging().sendToDevice(tokens, payload);
    
    }

    return null;
    
}

async function sendNewProjectApplicationNotification(projectId,responseId){

    const projectAuthor = await getProjectAuthorId(projectId);

    const appliedUserName = await getUsername(responseId);

    const tokens = await getDeviceTokens(projectAuthor);

    if (tokens.length > 0) {

        // Notification details.

        // Create a notification
        let payload = {
            data: {
                actionType: actionTypeNewResponse,
                title: notificationTitle,
                body: `${appliedUserName} applied to your project`,
                postId: projectId,
                responseId: responseId
            },
        };

        console.log("send notification to : ",tokens[0]);
    
        // Send notifications to all tokens.
    
        return admin.messaging().sendToDevice(tokens, payload);
    
    }

    return null;

}

async function getDeviceTokensForGroup(projectId){

    const snap = await admin.database().ref(`/groupchats/${projectId}/users`).once('value');

    if (snap.exists()) {

        const snap1 = await admin.database().ref(`/profiles`).once('value');

        let tokens = [];

        let users = snap.val();

        for(var i=0;i<users.length;i++){

            let user = users[i];

            let uid = user.userid;

            console.log(uid);

            if(snap1.exists()){

                let usertokens = Object.keys(snap1.val().uid.notificationTokens);

                for(var j=0;j<usertokens.length;j++){

                    tokens.push(usertokens[j]);

                }

            }

        }

        /*users.forEach(user => {

            let uid = user.userid;

            console.log(uid);

            if(snap1.exists()){

                Object.keys(snap1.val().uid.notificationTokens).forEach(token => {

                    tokens.push(token);
    
                });

            }

        });*/

        return tokens;
    
    }
    
    return [];

}

async function sendGroupMessageNotification(projectId,fromId,messageId){

    const fromUsername = await getUsername(fromId);

    const tokens = await getDeviceTokensForGroup(projectId);

    if (tokens.length > 0) {

        // Notification details.

        // Create a notification
        let payload = {
            data: {
                actionType: actionTypeNewGroupMessage,
                title: notificationTitle,
                body: `${fromUsername} messaged in your project group`,
                //icon: postImagePath,
                fromId: fromId
            },
        };

        console.log("send notification to : ",tokens[0]);
    
        // Send notifications to all tokens.
    
        return admin.messaging().sendToDevice(tokens, payload);
    
    }

    return null;


}

async function sendMessageNotification(toId,fromId,messageId){

    const fromUsername = await getUsername(fromId);

    const tokens = await getDeviceTokens(toId);

    if (tokens.length > 0) {

        // Notification details.

        // Create a notification
        let payload = {
            data: {
                actionType: actionTypeNewMessage,
                title: notificationTitle,
                body: `${fromUsername} messaged you`,
                fromId: fromId,
                toId: toId
            },
        };

        console.log("send notification to : ",tokens[0]);
    
        // Send notifications to all tokens.
    
        return admin.messaging().sendToDevice(tokens, payload);
    
    }

    return null;

}

async function getPostAuthorId(postId){

    const snap = await admin.database().ref(`/posts/${postId}`).once('value');

    if (snap.exists()) {

        return snap.val().authorId;
    
    }
    
    return null;

}

async function getUsername(uid){

    const snap = await admin.database().ref(`/profiles/${uid}`).once('value');

    if (snap.exists()) {

        return snap.val().username;
    
    }
    
    return null;

}

async function getDeviceTokens(uid){

    const snap = await admin.database().ref(`/profiles/${uid}/notificationTokens`).once('value');

    if (snap.exists()) {

        return Object.keys(snap.val());
    
    }
    
    return [];

}

async function getProjectImagePath(postId){

    const snap = await admin.database().ref(`/projects/${postId}`).once('value');

    if (snap.exists()) {

        return snap.val().imageTitle;
    
    }
    
    return null;

}

async function getPostImagePath(postId){

    const snap = await admin.database().ref(`/posts/${postId}`).once('value');

    if (snap.exists()) {

        return snap.val().imageTitle;
    
    }
    
    return null;

}

async function sendPostCommentNotification(postId,likeAuthorId,likeId){

    const postAuthorId = await getPostAuthorId(postId);

    if (likeAuthorId === postAuthorId){
        console.log('User commented own post');
        return 'User commented own post';
    }

    const postImagePath = await getPostImagePath(postId);

    const likeAuthorUsername = await getUsername(likeAuthorId);

    const tokens = await getDeviceTokens(postAuthorId);

    if (tokens.length > 0) {

        // Notification details.

        // Create a notification
        let payload = {
            data: {
                actionType: actionTypeNewPostComment,
                title: notificationTitle,
                body: `${likeAuthorUsername} commented on your post`,
                icon: postImagePath,
                postId: postId
            },
        };
    
        // Send notifications to all tokens.
    
        return admin.messaging().sendToDevice(tokens, payload);
    
    }

    return null;

}

async function sendPostLikeNotification(postId,likeAuthorId,likeId){

    const postAuthorId = await getPostAuthorId(postId);

    if (likeAuthorId === postAuthorId){
        console.log('User liked own post');
        return 'User liked own post';
    }

    const postImagePath = await getPostImagePath(postId);

    const likeAuthorUsername = await getUsername(likeAuthorId);

    const tokens = await getDeviceTokens(postAuthorId);

    if (tokens.length > 0) {

        // Notification details.

        // Create a notification
        let payload = {
            data: {
                actionType: actionTypeNewPostLike,
                title: notificationTitle,
                body: `${likeAuthorUsername} liked your post`,
                icon: postImagePath,
                postId: postId
            },
        };
    
        // Send notifications to all tokens.
    
        return admin.messaging().sendToDevice(tokens, payload);
    
    }

    return null;

}

async function getProjectAuthorId(postId){

    const snap = await admin.database().ref(`/projects/${postId}`).once('value');

    if (snap.exists()) {

        return snap.val().authorId;
    
    }
    
    return null;

}

async function sendProjectCommentNotification(postId,likeAuthorId,likeId){

    const postAuthorId = await getProjectAuthorId(postId);

    if (likeAuthorId === postAuthorId){
        console.log('User commented own project');
        return 'User commented own project';
    }

    const postImagePath = await getProjectImagePath(postId);

    const likeAuthorUsername = await getUsername(likeAuthorId);

    const tokens = await getDeviceTokens(postAuthorId);

    if (tokens.length > 0) {

        // Notification details.

        // Create a notification
        let payload = {
            data: {
                actionType: actionTypeNewProjectComment,
                title: notificationTitle,
                body: `${likeAuthorUsername} commented on your project`,
                icon: postImagePath,
                postId: postId
            },
        };
    
        // Send notifications to all tokens.
    
        return admin.messaging().sendToDevice(tokens, payload);
    
    }

    return null;

}

async function sendProjectLikeNotification(postId,likeAuthorId,likeId){

    const postAuthorId = await getProjectAuthorId(postId);

    if (likeAuthorId === postAuthorId){
        console.log('User liked own post');
        return 'User liked own post';
    }

    const postImagePath = await getProjectImagePath(postId);

    const likeAuthorUsername = await getUsername(likeAuthorId);

    const tokens = await getDeviceTokens(postAuthorId);

    if (tokens.length > 0) {

        // Notification details.

        // Create a notification
        let payload = {
            data: {
                actionType: actionTypeNewProjectLike,
                title: notificationTitle,
                body: `${likeAuthorUsername} liked your project`,
                icon: postImagePath,
                postId: postId
            },
        };
    
        // Send notifications to all tokens.
    
        return admin.messaging().sendToDevice(tokens, payload);
    
    }

    return null;

}

function buildContactPushMessage (title, theBody, clickAction, sound, senderId, recipientId) {

    let data = {
        chat_sdk_contact_entity_id: senderId,
    };

    return buildMessage(title, theBody, clickAction, sound, data, recipientId);

}

function buildMessagePushMessage (title, theBody, clickAction, sound, type, senderId, threadId, recipientId) {

    // Make the token payload
    let body = theBody;

    let messageType = parseInt(type);
    if (messageType === 1) {
        body = "Location";
    }
    if (messageType === 2) {
        body = "Image";
    }
    if (messageType === 3) {
        body = "Audio";
    }
    if (messageType === 4) {
        body = "Video";
    }
    if (messageType === 5) {
        body = "System";
    }
    if (messageType === 6) {
        body = "Sticker";
    }
    // if (messageType === 7) {
    //     body = "File";
    // }

    let data = {
        chat_sdk_thread_entity_id: threadId,
        chat_sdk_user_entity_id: senderId,
        chat_sdk_push_title: title,
        chat_sdk_push_body: body,
    };

    return buildMessage(title, body, clickAction, sound, data, recipientId);

}

function buildMessage (title, body, clickAction, sound, data, recipientId) {

    if (enableV4Compatibility) {
        // Make the user ID safe
        recipientId = recipientId.split(".").join("1");
        recipientId = recipientId.split("%2E").join("1");
        recipientId = recipientId.split("@").join("2");
        recipientId = recipientId.split("%40").join("2");
        recipientId = recipientId.split(":").join("3");
        recipientId = recipientId.split("%3A").join("3");
    } else {
        recipientId = md5(recipientId);
    }

    return {
        data: data,
        apns: {
            headers: {
            },
            payload: {
                aps: {
                    alert: {
                        title: title,
                        body: body
                    },
                    badge: 1,
                    sound: sound,
                    priority: "high",
                    category: clickAction,
                }
            },
        },
        topic: recipientId,
    };
}

function logData (data) {
    if (loggingEnabled) {
        const entry = log.entry({}, data);
        log.write(entry);
    }
}

function getUserName(usersRef, userId) {
    return getUserMeta(usersRef, userId).then(meta => {
        return getUserNameFromMeta(meta);
    });
}

function getUserIds (threadRef, senderId) {
    return threadRef.child('users').once('value').then((users) => {

        let IDs = [];

        let usersVal = users.val();
        if (usersVal !== null) {
            for (let userID in usersVal) {
                if (usersVal.hasOwnProperty(userID)) {
                    logData("UsersVal:" + usersVal);
                    let muted = usersVal[userID]["mute"];
                    logData("Muted:" + muted);
                    if (userID !== senderId && muted !== true) {
                        IDs.push(userID);
                    } else {
                        logData("Pushes muted for:" + userID);
                    }
                }
            }
        }

        return IDs;
    });
}

function unORNull(object) {
    return object === 'undefined' || object === null || !object;
}

function getUserMeta (usersRef, userId) {
    return usersRef.child(userId).child('meta').once('value').then((meta) => {
        let metaValue = meta.val();
        if (metaValue !== null) {
            return metaValue;
        }
        return null;
    });
}

function isBlocked (usersRef, recipientId, testId) {
    if (blockedUsersEnabled) {
        return usersRef.child(recipientId).child('blocked').child(testId).once('value').then((blocked) => {
            let blockedValue = blocked.val();
            logData("recipient: " + recipientId + ", testId: "+ testId + "val: " + JSON.stringify(blockedValue) + ", send push: " + blockedValue === null);
            return blockedValue !== null;
        });
    } else {
        return Promise.resolve(false);
    }
}

function getBlockedUsers (usersRef, userId) {
    if (blockedUsersEnabled) {
        return usersRef.child(userId).child('blocked').once('value').then((blocked) => {
            let blockedValue = blocked.val();

            // logData("usersRef " + usersRef.toString() + " uid: " + userId + ", val: " + blockedValue);

            if (blockedValue !== null) {
                return blockedValue;
            }
            return null;
        });
    } else {
        return Promise.resolve(null);
    }
}

function getUserNameFromMeta (metaValue) {
    if (metaValue !== null) {
        let name = metaValue["name"];
        if(unORNull(name)) {
            name = "Unknown";
        }
        return name;
    }
    return null;
}

exports.pushToChannels = functions.https.onCall((data, context) => {

    let body = data.body;

    let action = data.action;
    if(!action) {
        action = iOSAction;
    }

    let sound = data.sound;
    if(!sound) {
        sound = Sound;
    }

    let type = data.type;
    let senderId = String(data.senderId);
    let threadId = String(data.threadId);

    let userIds = data.userIds;

    if(unORNull(senderId)) {
        throw new functions.https.HttpsError("invalid-argument", "Sender ID not valid");
    }

    if(unORNull(threadId)) {
        throw new functions.https.HttpsError("invalid-argument", "Sender ID not valid");
    }

    if(unORNull(body)) {
        throw new functions.https.HttpsError("invalid-argument", "Sender ID not valid");
    }

    var status = {};
    for(let uid in userIds) {
        if(userIds.hasOwnProperty(uid)) {
            let userName = userIds[uid];
            let message = buildMessage(userName, body, action, sound, type, senderId, threadId, uid);
            status[uid] = message;
            admin.messaging().send(message);
        }
    }
    // return Promise.all(promises);
    // return res.send(status);
    return status;

});

if (reciprocalContactsEnabled) {
    exports.contactListener = functions.database.ref('{rootPath}/users/{userId}/contacts/{contactId}').onCreate((contactSnapshot, context) => {

        const contactVal = contactSnapshot.val();
        const userId = context.params.userId;
        const contactId = context.params.contactId;
        const rootPath = context.params.rootPath;

        if (!unORNull(userId) && !unORNull(contactId)) {

            const ref = admin.database().ref(rootPath).child("users").child(contactId).child("contacts").child(userId);
            const usersRef = admin.database().ref(rootPath).child("users");

            return ref.set({type: 0}).then(() => {
                return getUserName(usersRef, userId).then(name => {
                    let message = buildContactPushMessage(name, "Added you as a contact", iOSAction, Sound, userId, contactId);
                    return admin.messaging().send(message).then(success => {
                        return success;
                    }).catch(error => {
                        console.log(error.message);
                    });
                });
            });
        }

    });
}

exports.pushListener = functions.database.ref('{rootPath}/threads/{threadId}/messages/{messageId}').onCreate((messageSnapshot, context) => {

    let messageValue = messageSnapshot.val();

    let senderId = messageValue["from"];
    
    if (enableV4Compatibility && unORNull(senderId)) {
        senderId = messageValue["user-firebase-id"];
    }

    if(unORNull(senderId)) {
        return 0;
    }

    let threadId = context.params.threadId;
    let rootPath = context.params.rootPath;

    let threadRef = admin.database().ref(rootPath).child("threads").child(threadId);
    let usersRef = admin.database().ref(rootPath).child("users");

    return getUserMeta(usersRef, senderId).then(meta => {
        return getUserIds(threadRef, senderId).then(IDs => {

            const name = getUserNameFromMeta(meta);

            const promises = [];
            for(let i in IDs) {
                let userId = IDs[i];
                promises.push(isBlocked(usersRef, userId, senderId).then(isBlocked => {
                    if (!isBlocked) {
                        let messageText;

                        let meta = messageValue["meta"];
                        if (!unORNull(meta)) {
                            messageText = meta["text"];
                        }

                        if (enableV4Compatibility && unORNull(messageText)) {
                            meta = messageValue["json_v2"];
                            if (!unORNull(meta)) {
                                messageText = meta["text"];
                            }
                        }

                        if (!unORNull(messageText)) {
                            let message = buildMessagePushMessage(name, messageText, iOSAction, Sound, messageValue['type'], senderId, threadId, userId);
                            logData("Send push: " + messageText + ", to: " + userId);
                            admin.messaging().send(message).then(success => {
                                return success;
                            }).catch(error => {
                                console.log(error.message);
                            });
                        }
                    } else {
                        logData("push blocked to: " + userId);
                    }
                    return 0;
                }));
            }
            return Promise.all(promises);
        });
    });

});
