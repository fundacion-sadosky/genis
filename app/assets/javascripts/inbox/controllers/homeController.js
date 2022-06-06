define([], function() {
    'use strict';

    function HomeController($scope, userService) {

        $scope.showInbox = userService.showNotifications();

    }
    
    return HomeController;

});
