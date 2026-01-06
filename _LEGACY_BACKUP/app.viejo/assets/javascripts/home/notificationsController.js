define([], function() {
'use strict';

function NotificationsController($scope, $log, notificationsService, userService, inboxService) {

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

}

return NotificationsController;

});
