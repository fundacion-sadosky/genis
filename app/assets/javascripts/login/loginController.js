define([], function() {
'use strict';

function LoginController($scope, $log, $location, userService) {


    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");

	$scope.credentials = {};

	$scope.showLogin = true;

	userService.onLogin(function() {
		$log.debug("hide login form");
		$location.path('/home');
		$scope.showLogin = false;
	});

	userService.onLogout(function() {
		$log.debug("show login form");
		$location.path('/');
		$scope.showLogin = true;
        $scope.loginform.$setPristine();
	});
    
	$scope.login = function(authenticate) {
		
		userService.authenticate(authenticate).then(function(){
			$scope.credentials = {};
		}, function(xhr/*, status, error*/){
			$scope.requestToken = undefined;
			$scope.unauthorized = true;
			$scope.serverTime = xhr.headers("Date");
		});

	};	
}

return LoginController;

});
