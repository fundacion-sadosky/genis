define([], function() {
'use strict';

function RequestAuthorizationInterceptor($q, $window, $log, totpPromptService, sensitiveOperations) {


	var isSensitive = function(r) {
		var ret = false;
		sensitiveOperations.forEach(function(item) {
			var res = new RegExp(item.resource);
			var act = new RegExp(item.action);
			if (res.test(r.url) && act.test(r.method)) {
				ret = true;
				return true;
			}
		});
		return ret;
	};
	
	return {
		request : function(request) {

			if (isSensitive(request)) {
				var deferredReq = $q.defer();
				totpPromptService.totpPromise().then(
					function(value) {
						request.headers['X-TOTP'] = value;
						deferredReq.resolve(request);
					},
					function() {
						deferredReq.reject({status: 499});
					}
				);
				return deferredReq.promise;
			} else {
				return request;
			}

		}
	};
	
}

return RequestAuthorizationInterceptor;

});
