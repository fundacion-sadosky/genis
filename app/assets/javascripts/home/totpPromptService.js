define([], function() {
'use strict';


function TotpPromptService($log , $rootScope, $q) {

	var defer;
	
	var listeners = [];

	var notifyListeners = function() {
		for (var i = 0; i < listeners.length; i++) {
			try {
				listeners[i]();
			} catch (e) {
				$log.error("Error while notifying totp request", e, listeners[i]);
			}
		}
	};

	this.onTotpPrompt = function(callback) {
		listeners.push(callback);
	};

	this.totpPromise = function() {
		defer = $q.defer();
		notifyListeners();
		return defer.promise;
	};
		
	this.resolveTotpPromise = function(totp) {
		defer.resolve(totp);
	};
		
	this.cancelTotpPromise = function() {
		defer.reject();
	};

}

return TotpPromptService;

});