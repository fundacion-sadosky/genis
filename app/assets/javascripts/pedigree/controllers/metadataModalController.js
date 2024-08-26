define(['angular'], function(angular) {
    'use strict';

    function metadataModalController($scope, alertService, $modalInstance, data, pedigreeService) {
        $scope.modo = data.modo;
        $scope.idCourtCase=  parseInt(data.idCCase);
        $scope.caseStatus = data.caseStatus;
        $scope.metadataProfile={};
        $scope.sex = pedigreeService.getAvailableSex();
        var person =angular.copy(data.personData);

        if($scope.modo === 'edit'){
            $scope.metadataProfile = data.personData;
        }



        $scope.limpiar = function () {
            $scope.metadataProfile={};
        };



        $scope.minDate = $scope.minDate ? null : new Date();
        $scope.maxDate = $scope.maxDate ? null : new Date();
        $scope.minDateNac = null;
        $scope.minDateHasta = $scope.minDateHasta ? null: new Date();

        $scope.inputVisible= function(fechaContraria){
                var fecha = $scope.metadataProfile[fechaContraria];
                if(fechaContraria === 'dateOfBirthFrom'){
                    $scope.minDateHasta =  fecha;
                }
                if(fecha === undefined || fecha === null ) {	return false;	}
                else {	return true;	}

        };

        $scope.checkMaxMin = function(fieldName, fechaMin){
            var aux = $scope.metadataProfile[fieldName];
            var min = $scope.metadataProfile[fechaMin];
            var max = new Date();

            if(min === undefined || min === null ) {
                min = $scope.metadataProfile.dateOfBirthFrom;
            }

            if(max-aux < 0 ){
                alertService.error({message: $.i18n.t('alerts.date.before')});
                $scope.metadataProfile[fieldName] = undefined;
            }else{
                if( min-aux > 0 ){
                    alertService.error({message: $.i18n.t('alerts.date.afterBirth')});
                    $scope.metadataProfile[fieldName] = undefined;
                }
            }

        };

        $scope.checkMax = function(fieldName) {
            var aux = $scope.metadataProfile[fieldName];
            var today = new Date();

            if(today-aux < 0 ){
                alertService.error({message: $.i18n.t('alerts.date.before')});
                $scope.metadataProfile[fieldName] = undefined;
            }else{
                if(fieldName === 'dateOfBirthFrom' || fieldName === 'dateOfBirth' ){
                    if(aux !== undefined || aux !== null) {$scope.minDateNac = aux; }
                    else { $scope.minDateNac = null; }
                }
            }

        };

        $scope.checkMin = function (fieldName) {
            var aux = $scope.metadataProfile[fieldName];
            var today = new Date();

            if(today-aux > 0){
                alertService.error({message: $.i18n.t('alerts.date.afterNow')});
                $scope.metadataProfile[fieldName] = undefined;
            }
        };

        $scope.comprobarFechas = function(fieldName, fechaMin){
            $scope.checkMax(fieldName);

            var aux = $scope.metadataProfile[fieldName];
            var min = $scope.metadataProfile[fechaMin];

            if(min -aux < 0 && min !== null){
                alertService.error({message: $.i18n.t('alerts.date.afterDisappearance')});
                $scope.metadataProfile[fieldName] = undefined;
            }
        };

        $scope.borrarValor= function(fechaContraria){
            if(!$scope.inputVisible(fechaContraria) ){
                $scope.metadataProfile.dateOfBirthTo = null;
                return true;
            }
        };

        $scope.guardar = function () {

            if($scope.modo === 'add'){
                pedigreeService.createMetadata($scope.idCourtCase,$scope.metadataProfile).then(function (response) {
                    $modalInstance.close(response.data);
                },function(response){
                    alertService.error({message: response.data, parent:'metadata'});
                });
            }else
            {
                pedigreeService.updateMetadata($scope.idCourtCase,$scope.metadataProfile).then(function(response){
                    $modalInstance.close(response.data);
                });
            }

        };

        $scope.cancel = function () {
           $scope.metadataProfile =  person;
            $modalInstance.dismiss();
        };

        $scope.isCourtCaseClosed = function() {
            return $scope.caseStatus === 'Closed';
        };

    }


return metadataModalController;
});