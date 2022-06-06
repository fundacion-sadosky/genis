define(['angular', './loadingAppController', './totpPromptService', './totpPromptController', './errorService', './errorController', './headerController', './notificationsService', './notificationsController'], 
function(angular, LoadingAppController, TotpPromptService, TotpPromptController, ErrorService, ErrorController, HeaderController, NotificationsService, NotificationsController) {
'use strict';

angular
	.module('pdg.home', [])
	.controller('loadingAppController', ['$scope', LoadingAppController])
	.service('totpPromptService', ['$log', '$rootScope', '$q', TotpPromptService])
	.controller('totpPromptController', ['$scope', 'totpPromptService', TotpPromptController])
	.service('errorService', ['$log', ErrorService]) 
	.controller('errorController', ['$scope', 'errorService', 'alertService', ErrorController])
	.controller('headerController', ['$scope', 'userService', '$location', '$modal', 'hotkeys','appConf', HeaderController])
	.service('notificationsService', ['$log','$rootScope', 'cryptoService', 'userService', NotificationsService])
	.controller('notificationsController', [ '$scope', '$log', 'notificationsService', 'userService', 'inboxService', NotificationsController])
    .config(['$routeProvider', function($routeProvider) {
		$routeProvider
			.when('/configuracion', {templateUrl: '/assets/javascripts/home/views/configuracion.html'});
			}]);


        return undefined;

});
