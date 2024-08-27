define(
	[],
	function() {
		'use strict';

		function HeaderController(
			$scope,
			userService,
			$location,
			$modal,
			$timeout,
			hotkeys,
			appConf
		) {
			var modalInstance = null;
			$scope.version = appConf.version;
			hotkeys.bindTo($scope).add({
				combo : 'ctrl+alt+b',
				allowIn : [ 'INPUT', 'SELECT', 'TEXTAREA' ],
				callback : function() {
					modalInstance = $modal.open({
						templateUrl : '/assets/javascripts/home/views/ee.html',
						scope : $scope
					});
				}
			});

			// Wrap the current user from the service in a watch expression
			$scope.$watch(
				function() {
					var user = userService.getUser();
					return user;
				},
				function(user) {
					$scope.user = user;
				},
				true
			);

			$scope.showAbout = function() {
				modalInstance = $modal.open({
					templateUrl : '/assets/javascripts/home/views/aboutModal.html',
					scope : $scope
				});
			};

			$scope.closeModal = function() {
				modalInstance.close();
			};

			$scope.logout = function() {
				$scope.selectedMenu = undefined;
				userService.logout();
				$scope.user = undefined;
			};

			var set_menu_titles = function() {
				$scope.menu_profiles= "menu.profiles";
				$scope.menu_profilesList= "menu.profilesList";
				$scope.menu_bulkUpload= "menu.bulkUpload";
				$scope.menu_bulkAcceptance= "menu.bulkAcceptance";
				$scope.menu_inferiorInstanceProfileApproval= "menu.inferiorInstanceProfileApproval";
				$scope.menu_profileComparison= "menu.profileComparison";
				$scope.menu_profileExporter= "menu.profileExporter";
				$scope.menu_profileLIMSExporter= "menu.profileLIMSExporter";
				$scope.menu_mpiDvi= "menu.mpiDvi";
				$scope.menu_matches= "menu.matches";
				$scope.menu_forensic= "menu.forensic";
				$scope.menu_mpi= "menu.mpi";
				$scope.menu_monitoring= "menu.monitoring";
				$scope.menu_reports= "menu.reports";
				$scope.menu_operationLog= "menu.operationLog";
				$scope.menu_interconnection_inferiorInstances= "menu.interconnection.inferiorInstances";
				$scope.menu_configuration= "menu.configuration";
				$scope.menu_logout= "menu.logout";
				$scope.generics_close = "generics.close";
				$scope.about_version = "about.version";
			};
			set_menu_titles();

			var clear_menu_titles = function() {
				$scope.menu_profiles= "";
				$scope.menu_profilesList= "";
				$scope.menu_bulkUpload= "";
				$scope.menu_bulkAcceptance= "";
				$scope.menu_inferiorInstanceProfileApproval= "";
				$scope.menu_profileComparison= "";
				$scope.menu_profileExporter= "";
				$scope.menu_profileLIMSExporter= "";
				$scope.menu_mpiDvi= "";
				$scope.menu_matches= "";
				$scope.menu_forensic= "";
				$scope.menu_mpi= "";
				$scope.menu_monitoring= "";
				$scope.menu_reports= "";
				$scope.menu_operationLog= "";
				$scope.menu_interconnection_inferiorInstances= "";
				$scope.menu_configuration= "";
				$scope.menu_logout= "";
				$scope.generics_close = "";
				$scope.about_version = "";
			};

			$scope.$on(
				"i18nextLanguageChange",
				function(event, args) {
					clear_menu_titles();
					$timeout(
						function() {
							set_menu_titles();
						},
						1
					);
				}
			);

		}
		return HeaderController;
	}
);
