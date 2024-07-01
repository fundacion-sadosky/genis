define(['angular','lodash'], function(angular,_) {
'use strict';

function CourtCasesDataController ($scope, userService, pedigreeService, $routeParams, $location, profileDataService, $route, alertService, $modal) {
	$scope.courtcase = {};
	$scope.showHeader = $location.path().includes('courtcasesdata');
    $scope.personData= $scope.courtcase.personData;
    $scope.pageSize= 10;
    $scope.totalItemsData = 0;
    $scope.busquedaInput = {};
    $scope.busquedaInput.busquedaAlias = "";


    $scope.sex = pedigreeService.getAvailableSex();
	
	profileDataService.getCrimeTypes().then(function(response) {
		$scope.crimeTypes = response.data;
	});
    
	function getCourtCase(id) {
        var totalMetadata = {};
		pedigreeService.getCourtCaseFull(id).then(function(response){
            $scope.currentPageNumber = 1;
			$scope.courtcase = response.data;
			$scope.mode = 'update';
            $scope.personData = $scope.courtcase.personData;

			totalMetadata.pageSize= $scope.pageSize;
			totalMetadata.page = 0;
			totalMetadata.idCourtCase = parseInt($scope.courtcaseId);
			totalMetadata.input= $scope.busquedaInput.busquedaAlias;

			pedigreeService.getTotalMetadata(totalMetadata).then(function (response){
                $scope.totalItemsData = response.data;
			} );

            pedigreeService.getMetadata(totalMetadata,parseInt($scope.courtcaseId)).then(function (response) {
                    $scope.personData = response.data;
            });

		});
	}
	
	if ($routeParams.id) {
		getCourtCase($routeParams.id);
	}
	
	userService.getGeneticistUsers().then(function(response) {
		var geneticistUsersDistinct = [];

		angular.forEach(response.data, function(value, index){
			var exists = false;
			angular.forEach(geneticistUsersDistinct, function(geneticist, i){
				if (geneticistUsersDistinct[i].id === response.data[index].id){
					exists = true;
				}
			});

			if (!exists){
				geneticistUsersDistinct.push(response.data[index]);
			}
		});

		$scope.geneticistUsers = geneticistUsersDistinct;
	});

	pedigreeService.getCaseTypes().then(function (response) {
		$scope.caseTypes = response.data;
	});

	function afterSave(id){
		alertService.success({message: $.i18n.t('alerts.genericSuccess.operation')});
        $location.path('/court-case/' + id);
        }
	
	$scope.save = function() {
		if ($scope.courtcase.id) {
			pedigreeService.updateCourtCase($scope.courtcase).then(function(response){
				afterSave(response.data);
			});
		} else {
			pedigreeService.createCourtCase($scope.courtcase).then(function(response){
				afterSave(response.data);
			},function(response){
               alertService.error({message: response.data});
            });
		}

	};
	
	$scope.cancel = function(){
		$route.reload();
	};


    $scope.borrarValor= function(fechaContraria){
		if(!$scope.inputVisible(fechaContraria) && $scope.courtcase.personData !== undefined){
            $scope.courtcase.personData.dateOfBirthTo = null;
			return true;
		}
	};

    $scope.newMetadata= function(modo){
		$modal.open({
            templateUrl: '/assets/javascripts/pedigree/views/metadata-modal.html',
            controller: 'metadataModalController',
            resolve: {
                data: function () {
                    return {
                        idCCase:$scope.courtcaseId,
                        modo: modo
                    };
                }
            },
            backdrop: 'static'
        }).result.then(function(response){
            afterSave(response);
            getCourtCase(response);
		});
	};


    $scope.borrar=function(person){
            pedigreeService.removeMetadata(parseInt($scope.courtcaseId),person).then(function(response){
                console.log('delet Metadata response',response);
				getCourtCase(parseInt($scope.courtcaseId));
                alertService.success({message: $.i18n.t('alerts.register.deleted')});

                },function(response){
            console.log('delet Profiles response',response);
            alertService.error({message: $.i18n.t('alerts.register.deletedError')});
        });

    };

    $scope.editMetadata= function(pd){

        $modal.open({
            templateUrl: '/assets/javascripts/pedigree/views/metadata-modal.html',
            controller: 'metadataModalController',
            resolve: {
                data: function () {
                    return {
                        idCCase:$scope.courtcaseId,
                        modo: 'edit',
                        caseStatus: $scope.courtcase.status,
						personData: angular.copy(pd)
                    };
                }
            },
           backdrop: 'static'
        }).result.then(function(response){
            afterSave(response);
            getCourtCase(response);
        });
    };

    $scope.changePage = function() {
        $scope.filtrar();
    };

    $scope.filtrar = function () {
            var totalMetadata = {};
            totalMetadata.idCourtCase = parseInt($scope.courtcaseId);
            totalMetadata.pageSize = $scope.pageSize;

            totalMetadata.input = $scope.busquedaInput.busquedaAlias;

            if($scope.currentPageNumber){
                totalMetadata.page  = $scope.currentPageNumber -1;
            }else{
                totalMetadata.page = 0;
            }

            pedigreeService.getTotalMetadata(totalMetadata).then(function (response){
                $scope.totalItemsData= response.data;
            } );

            pedigreeService.getMetadata(totalMetadata,parseInt($scope.courtcaseId)).then(function (response) {
               if(response.data.length=== 0) {
                   $scope.noResult = true;
               }else{
                   $scope.noResult = false;
                   $scope.personData = response.data;
                }
            });


    };



	$scope.isClosed = function() {
		return !_.isUndefined($scope.courtcase) && $scope.courtcase.status === 'Closed';
	};

}

return CourtCasesDataController;

});