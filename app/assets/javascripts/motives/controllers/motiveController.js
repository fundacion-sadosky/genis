define(['lodash'], function(_) {
    'use strict';

    function MotiveController($scope, $modal, motiveService,alertService) {

        $scope.init = function() {
            motiveService.getMotivesTypes().then(function(response) {
                $scope.motivesTypes = response.data;
                if(!$scope.selectedMotiveType){
                    $scope.selectedMotiveType = $scope.motivesTypes[0].id;
                }
            }, function() {
                alertService.error({message: $.i18n.t('error.common')});
            });
        };

        $scope.init();

        $scope.doInsert = function(mType){
            console.log(mType);
            $scope.selectedMotive = {};
            $scope.modalInstance = $modal.open({
                templateUrl:'/assets/javascripts/motives/views/insert.html',
                scope: $scope,
                keyboard: false
            });
        };

        var giveUpdateModal = function(motivo){
            $scope.selectedMotive = motivo;
            $scope.selectedMotiveCopy = _.cloneDeep(motivo);
            $scope.modalInstance = $modal.open({
                templateUrl:'/assets/javascripts/motives/views/update.html',
                scope: $scope,
                keyboard: false
            });
        };

        $scope.saveMotive = function(isAnInsertion){
            $scope.closeModal();
            if (isAnInsertion) {
                var nuevoMotivo = {};
                nuevoMotivo.id = 0;
                nuevoMotivo.freeText = false;
                nuevoMotivo.motiveType = parseInt($scope.selectedMotiveType);
                nuevoMotivo.description = $scope.selectedMotive.description;
                console.log(nuevoMotivo);
                motiveService.insertMotive(nuevoMotivo).then(function(data) {
                    if(!$scope.motives) {
                        $scope.motives = [];
                    }
                    if(data.data.id){
                        nuevoMotivo.id = data.data.id;
                        $scope.motives.push(nuevoMotivo);
                    }
                    alertService.success({message: $.i18n.t('motives.addSuccess')});
                }, function() {
                    alertService.error({message: $.i18n.t('error.common')});
                });

            }else{
                $scope.selectedMotive.description = $scope.selectedMotiveCopy.description;
                motiveService.updateMotive($scope.selectedMotive).then(function() {
                    alertService.success({message: $.i18n.t('motives.editSuccess')});
                }, function() {
                    alertService.error({message: $.i18n.t('error.common')});
                });
            }

        };
        $scope.doUpdate = function(motivo){
            giveUpdateModal(motivo);
        };
        $scope.doDelete = function (confirmRes,motivo) {
            if (!confirmRes) {
                return;
            }
            var parsedMotivo = JSON.parse(motivo);
            if(parsedMotivo.id){
                motiveService.deleteMotiveById(parsedMotivo.id).then(function() {

                    if($scope.motives && $scope.motives.constructor === Array){
                        for (var i = 0; i < $scope.motives.length; i++) {
                            if($scope.motives[i].id === parsedMotivo.id){
                                $scope.motives.splice(i,1);
                            }
                        }
                    }

                    alertService.success({message: $.i18n.t('motives.deleteSuccess')});
                }, function() {
                    alertService.error({message: $.i18n.t('error.common')});
                });
            }else{
                alertService.error({message: $.i18n.t('error.common')});
            }
        };
        $scope.updateMotiveType = function() {
            $scope.showAdd = true;
            motiveService.getMotives($scope.selectedMotiveType).then(function(response) {
                $scope.motives = response.data;
            }, function() {
                $scope.motives = [];
            });
        };

        $scope.closeModal = function(){
            $scope.modalInstance.close();
        };

    }

    return MotiveController;

});