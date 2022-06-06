define([], function() {
'use strict';

function ResponseErrorInterceptor($q, $location, $log, $cookies, $injector, errorService) {

	return {
		response : function(response) {
			return response || $q.when(response);
		},
		responseError : function(rejection) {
			if (rejection.status === 401) {
				$log.info('Ok we got 401 ');
				$injector.get('userService').logout();
			} else if (rejection.status === 500) {
				errorService.setError(rejection);
			} else if (rejection.status === 403) {
				var error = {
					"data" : '<p style="background: #FF4D4D; height:50px; text-align:center; font-size:24px;"><b>No tiene los permisos necesarios para acceder al recurso</b></p>'
				};
				errorService.setError(error);
			}
			return $q.reject(rejection);
		}
	};

}

return ResponseErrorInterceptor;

});
