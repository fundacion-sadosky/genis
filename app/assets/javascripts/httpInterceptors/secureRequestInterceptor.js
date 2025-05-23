define([],function() {
'use strict';

function SecureRequestInterceptor($q, $window, $log, $injector) {

	return {
		request : function(request) {
			request.headers = request.headers || {};

			var isCategoriesImport = request.url.indexOf("/categories/import") === 0;
			var isKitsImport = request.url.indexOf("/strkit/import") === 0;
			var isLocusImport = request.url.indexOf("/locus/import") === 0;
			var isRolesImport = request.url.indexOf("/roles/import") === 0;


			var isPublicAsset = request.url.indexOf("/assets") === 0;

			var isJsLibAsset = 
				(request.url.indexOf("template") === 0) || 
				(request.url.indexOf("bootstrap") === 0);

			var isSensitiveOpList = request.url.indexOf("/sensitive-operations") === 0;

			var isLogin = request.url.indexOf("/login") === 0;

			var isSignup = request.url.indexOf("/signup") === 0;

			var isClearPass = request.url.indexOf("/clear-password") === 0;

			var isDisclaimer = request.url.indexOf("/disclaimer") === 0;

			var isRolesForSignUp = request.url.indexOf("/rolesForSU") === 0;

			var isSecuredPath = !(
				isPublicAsset ||
				isJsLibAsset ||
				isSensitiveOpList ||
				isLogin ||
				isSignup ||
				isRolesForSignUp ||
				isClearPass ||
				isDisclaimer ||
				isCategoriesImport ||
				isKitsImport ||
				isLocusImport ||
				isRolesImport
			);

			if (isSecuredPath && !request.file) {
				// non public assets
				$log.log("original request", request.url, request);
				request = $injector.get('cryptoService').encryptRequest(request);
				$log.log("encryptedRequest", request.url, request);
			}
			return request;
		}
	};
}

return SecureRequestInterceptor;

});
