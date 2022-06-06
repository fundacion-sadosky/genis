define([], function() {
'use strict';

function ErrorService($log) {

	var listeners = [];

	this.setError = function(err) {
		for (var i = 0; i < listeners.length; i++) {
			try {
				listeners[i](err);
			} catch (e) {
				$log.error("Error while notifying error", e, listeners[i]);
			}
		}
	};

	this.onError = function(callback) {
		listeners.push(callback);
	};
}

return ErrorService;

});