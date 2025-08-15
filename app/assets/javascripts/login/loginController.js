define([], function() {
'use strict';

function LoginController($scope, $log, $location, userService, profileDataService, profileService) {


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

	$scope.removeDesktopProfiles = function() {
		profileDataService.getDesktopProfiles().then(function(profiles) {
			console.debug("removing desktop profiles: ", profiles.data);
			profiles.data.forEach(function(profile) {
				profileService.removeProfile(profile).then(function () {
					console.log("Desktop profile removed:", profile);
				});

				profileDataService.removeProfile(profile).then(function () {
					console.log("Desktop profile data removed:", profile);
				});
			});
		});
	};


	$scope.login = function(authenticate) {
		
		userService.authenticate(authenticate).then(function(){
			$scope.credentials = {};
			$scope.removeDesktopProfiles();
		}, function(xhr/*, status, error*/){
			$scope.requestToken = undefined;
			$scope.unauthorized = true;
			$scope.serverTime = xhr.headers("Date");
		});

	};	
}

return LoginController;

});
