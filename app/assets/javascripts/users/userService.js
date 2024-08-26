define(['angular'], function(ng) {
'use strict';

function UserService(playRoutes, $cookies, $window, $log) {
	
	var getUserFromSessionStorage = function() {
		var user;
		if($window.sessionStorage.user){
			var usr = JSON.parse($window.sessionStorage.user);		

			user = {
				name: usr.userDetail.id,	
				geneMapperId: usr.userDetail.geneMapperId,
				permissions: usr.userDetail.permissions,	
				credentials: usr.credentials,
				superuser: usr.userDetail.superuser
			}; 

		} 
		return user;
	};

	var setUserIntoSessionStorage = function(response) {
		$window.sessionStorage.user = JSON.stringify(response);
	};

	var user = getUserFromSessionStorage();
	
	var loginListeners = [];

	var fireLogin = function(user) {
		for (var i = 0; i < loginListeners.length; i++) {
			try {
				loginListeners[i](user);
			} catch (e) {
				$log.error("Error while notifying login", e, loginListeners[i]);
			}
		}
	};

	this.onLogin = function(callback) {
		loginListeners.push(callback);
		if(user) {
			callback(user);
		}
	};

	var logoutListeners = [];

	var fireLogout = function() {
		for (var i = 0; i < logoutListeners.length; i++) {
			try {
				logoutListeners[i]();
			} catch (e) {
				$log.error("Error while notifying logout", e, logoutListeners[i]);
			}
		}
	};

	this.onLogout = function(callback) {
		logoutListeners.push(callback);
	};

	this.authenticate = function(credentials) {
			
		var authenticationRequest = credentials;

		return playRoutes.controllers.Authentication.login().post(authenticationRequest)
				.success(
					function(response){
						setUserIntoSessionStorage(response);
						user = getUserFromSessionStorage();
						fireLogin(user);
					}
				).error(function(){
					logout();
				}
			);
	};
		
	var logout = function() {
		delete $cookies['X-USER'];
		$window.sessionStorage.clear();
		fireLogout();
		user = null;
	};
		
	this.logout = logout; 

	this.getUser = function() {
		return user;
	};

    this.fixPhone2 = function(user) {
        if (user.phone2 !== undefined && user.phone2.length === 0) {
            user.phone2 = undefined;
        }
    };

    this.signupRequest = function(solicitude){
        var req = ng.copy(solicitude);
        var roles = [];
        for ( var roleId in solicitude.roles) {
            if(solicitude.roles[roleId]) {
                roles.push(roleId);
            }
        }
        req.roles = roles;

        this.fixPhone2(req);

        return playRoutes.controllers.Users.signupRequest().post(req);
    };
    this.clearPassRequest = function(solicitude){
        var req = ng.copy(solicitude);

        return playRoutes.controllers.Users.clearPassRequest().post(req);
    };
    this.getDisclaimerHtml = function(){
        return playRoutes.controllers.DisclaimerController.getDisclaimer().get();

    };
    this.clearPassConfirmation = function(challenge){
		var confirmationRequest = {};

		if(challenge){
			confirmationRequest.clearPassRequestId = challenge.clearPassRequestId;
			confirmationRequest.challengeResponse = challenge.challengeResponse;
		}

        return playRoutes.controllers.Users.clearPassConfirmation().put(confirmationRequest);
    };
	this.signupConfirmation = function(challenge){
		return playRoutes.controllers.Users.signupConfirmation().put(challenge);
	};

	this.listUsers = function(){
		return playRoutes.controllers.Users.listUsers().get();
	};

	this.updateUserSatus = function(userName, status){
		return playRoutes.controllers.Users.setStatus(userName).put(status);
	};
	
	this.updateUser = function(user){
        this.fixPhone2(user);
		return playRoutes.controllers.Users.updateUser().put(user);
	};
	
	this.getGeneticistUsers = function() {
		return playRoutes.controllers.Geneticists.getGeneticistUsers().get();
	};
    
    var hasPermission =  function(permission) {
        return user.permissions.indexOf(permission.trim()) > -1;
    };
	this.hasPermission = hasPermission;
    
    var hasAnyPermission = function(permissions) {
        var pList = permissions.split(',');
        return pList.some(function(p){return hasPermission(p);});
    };
    this.hasAnyPermission = hasAnyPermission;
    
    this.showNotifications = function() {
        return hasAnyPermission("DNA_PROFILE_CRUD, MATCHES_MANAGER, PROTOPROFILE_BULK_UPLOAD, PROTOPROFILE_BULK_ACCEPTANCE, USER_CRUD, PEDIGREE_CRUD");
    };

    this.setLanguage = function(lang) {
      var x = playRoutes.controllers.Application.changeLanguage(lang).get();
    };
}

return UserService;

});