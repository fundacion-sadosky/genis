define([], function() {
	'use strict';

	function SaveCtrl ($scope, $log, laboratoriesService, alertService) {
		$scope.regexEmail = new RegExp("^[_\\w-\\+]+(\\.[_\\w-]+)*@[\\w-]+(\\.\\w+)*(\\.[A-Za-z]{2,})$");

		if ($scope.laboratory.code){
			laboratoriesService.getLaboratory($scope.laboratory.code).then(function(response){
				$scope.laboratory = response.data;
				$scope.giveProvinces($scope.laboratory.country);
				$scope.isNew = false;
			});
		}

		laboratoriesService.getCountries().then(function(response){
			$scope.countries = response.data;
		});

		$scope.giveProvinces = function(country) {
			laboratoriesService.getProvinces(country).then(function(response){
				$scope.provinces = response.data;
			});
		};

		$scope.closeModal = function(){
			$scope.clearLaboratory();
			$scope.labForm.$setPristine();
			$scope.modalInstance.close();
		};

		$scope.saveLaboratory = function(isAnInsertion){
			if (isAnInsertion) {
				laboratoriesService.createLaboratory($scope.laboratory).then(function (response) {
					if (response.data.length > 0) {
						alertService.success({message: 'Fue dado de alta con exito'});
					} else {
						alertService.error({message: 'No pudo ser dado de alta'});
					}

					$scope.clearLaboratory();
					$scope.labForm.$setPristine();
					$scope.refreshLabs();
				},
				function (response) {
                    if (response.status !== 499) {
                        $log.log(response);

                        var e = response.data;
                        var errores = (e.indexOf("duplicate key") > -1 && e.indexOf("CODE_NAME") > -1) ? "El código del laboratorio ya existe" : e;
                        alertService.error({message: ' No pudo ser dado de alta: ' + errores});
                    }
				});
			} else{
				laboratoriesService.updateLaboratory($scope.laboratory).then(function (response) {
						if (response.data.length > 0) {
							alertService.success({message: 'El laboratorio se ha actualizado correctamente'});
						} else {
							alertService.error({message: ' El laboratorio no pudo ser actualizado'});
						}

						$scope.laboratory = undefined;
						$scope.labForm.$setPristine();
						$scope.refreshLabs();
						$scope.closeModal();
					},
					function (response) {
                        if (response.status !== 499) {
                            $log.log(response);

                            var e = response.data;
                            var errores = (e.indexOf("duplicate key") > -1 && e.indexOf("CODE_NAME") > -1) ? "El código del laboratorio ya existe" : e;
                            alertService.error({message: ' El laboratorio no pudo ser actualizado: ' + errores});
                        }
					});
			}
		};

		$scope.cancelForm = function(){
			$scope.clearLaboratory();
			$scope.labForm.$setPristine();
		};
	}
	
	return SaveCtrl;
});