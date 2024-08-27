define([], function() {
'use strict';

function NotificationsController($scope, $rootScope, $log, $timeout, $route, $i18next, notificationsService, userService, inboxService) {

	$scope.notiCount = 0;
	$scope.stall = true;
    $scope.pedigreeStall = true;
    
    userService.onLogin(function (user) {
        if (userService.showNotifications()) {
            inboxService.count({page: 0, pageSize: 30, user: user.name, pending: true}).then(function (response) {
                $scope.notiCount = response.headers('X-NOTIF-LENGTH');
            });
        } else {
            $scope.notiCount = 0;
        }
    });

	notificationsService.onMatchStatusNotification(function(msg){
		var status = msg.status;
		if (status === "started"){
			$scope.stall = false;
			$scope.working = true;
			$scope.fail = false;
		}else if (status === "ended"){
			$scope.stall = true;
			$scope.working = false;
			$scope.fail = false; 
		}else if (status === "fail"){
			$scope.stall = false;
			$scope.working = false;
			$scope.fail = true;
		}else if (status === "pedigreeStarted"){
			$scope.pedigreeStall = false;
			$scope.pedigreeWorking = true;
		} else if (status === "pedigreeEnded"){
            $scope.pedigreeStall = true;
            $scope.pedigreeWorking = false;
		}

		$scope.$apply();
	});
	
	notificationsService.onNotification(function(noti) {
		$log.debug(noti);
		if (noti.pending) {
			$scope.notiCount ++;
		} else {
			$scope.notiCount --;
		}
		$scope.$apply();	
	});

	$scope.getLanguage = function(){
		return $i18next.options.lng;
	};

	$scope.changeLanguage = function(lang){
		var nextLangFront = "en";
		var nextLangBack= "en";
		if ($scope.getLanguage() === "en") {
			nextLangFront = "es-AR";
			nextLangBack = "es";
		}
		if ($scope.getLanguage() === "es-AR") {
			nextLangFront = "en";
			nextLangBack = "en";
		}
		$i18next.options.lng = nextLangFront;
		// userService.setLanguage(nextLangBack);
		$scope.$emit("i18nextLanguageChange", { lang: nextLangFront });
		$route.reload();

	};

}

return NotificationsController;

});
