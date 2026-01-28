define([], function() {
	'use strict';

	function SaveCtrl ($scope, $log, kitService, alertService) {

		if ($scope.selectedKit.id){
			kitService.getFull($scope.selectedKit.id).then(function(response){
				$scope.selectedKit = response.data;
			});
		}

		$scope.closeModal = function(){
			$scope.clearSelectedKit();
			$scope.kitForm2.$setPristine();
			$scope.modalInstance.close();
		};

		$scope.updateKit = function(){
            kitService.updateKit($scope.selectedKit).then(
                function (response) {
                    if (response.data.length > 0) {
                        alertService.success({message: 'El kit se ha actualizado correctamente'});
                    } else {
                        alertService.error({message: ' El kit no pudo ser actualizado'});
                    }
                    $scope.selectedKit = undefined;
                    $scope.kitForm2.$setPristine();
                    $scope.loadKits();
                    $scope.closeModal();
                },
                function (response) {
                    if (response.status !== 499) {
                        $log.log(response);
                        alertService.error({message: ' El kit no pudo ser actualizado: ' + response.data});
                    }
                }
            );
		};

		$scope.cancelForm = function(){
			$scope.clearSelectedKit();
			$scope.kitForm2.$setPristine();
		};

	}

	return SaveCtrl;
});