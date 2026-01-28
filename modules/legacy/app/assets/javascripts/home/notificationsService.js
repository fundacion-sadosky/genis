define([],function() {
'use strict';

function NotificationsService($log, $rootScope, cryptoService, userService) {	
	
	/* global EventSource : true */
	
	var broadcastNotification = function(msg) {
		$log.info("notification recived: " + msg.data);
		var noti = JSON.parse(msg.data);
		for (var i = 0; i < listeners.length; i++) {
			try {
				listeners[i](noti);
			} catch (e) {
				$log.error("Error while notifying notification", e, listeners[i]);
			}
		}
	};

	var broadcastMatchStatus = function(msm){
		$log.info("match recived: " + msm.data);
		var nm = JSON.parse(msm.data);
		for (var i = 0; i < matchListeners.length; i++) {
			try {
				matchListeners[i](nm);
			} catch (e) {
				$log.error("Error while notifying notification", e, matchListeners[i]);
			}
		}
	};

    var feed;
    var matchFeed;

    userService.onLogin(function() {
        if (userService.showNotifications()) {
            if (feed) {
                feed.close();
                feed = null;
            }
            if (matchFeed) {
                matchFeed.close();
                matchFeed = null;
            }
            var urlmatchEcd = cryptoService.encryptBase64("/match-notifications");
            var urlEncoded = cryptoService.encryptBase64("/notifications");
            feed = new EventSource(urlEncoded);
            matchFeed = new EventSource(urlmatchEcd);
            feed.addEventListener("message", broadcastNotification, false);
            matchFeed.onmessage = broadcastMatchStatus;
            $log.info("listenForNotifications");
        }
    });

    userService.onLogout(function() {
        if(feed){
            feed.close();
            feed = null;
        }
        if(matchFeed){
            matchFeed.close();
            matchFeed = null;
        }
    });
    
	var listeners = [];

	var matchListeners = [];
	
	this.onMatchStatusNotification = function(callback) {
		matchListeners.push(callback);
	};
	
	this.onNotification = function(callback) {
		listeners.push(callback);
	};

}

return NotificationsService;

});