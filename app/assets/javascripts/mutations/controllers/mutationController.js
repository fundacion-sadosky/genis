define(['lodash'], function(_) {
    'use strict';

    function MutationController($scope, mutationModelService,alertService,$location) {

        $scope.init = function() {
            mutationModelService.getMutationModelsTypes().then(function(response) {
                $scope.mutationModelsTypes = response.data;
                if(!$scope.selectedMutationModelType){
                    $scope.selectedMutationModelType = $scope.mutationModelsTypes[0].id;
                }
                $scope.mutationModelsTypesDesc = _.keyBy($scope.mutationModelsTypes, function(o) {
                    return o.id+"";
                });
            }, function() {
                alertService.error({message: 'Error'});
            });
            $scope.showAdd = true;
            mutationModelService.getMutationModels().then(function(response) {
                $scope.mutationModels = response.data;
            }, function() {
                $scope.mutationModels = [];
            });
        };

        $scope.init();

        $scope.doInsert = function(){
            $location.url('/new-mutation-model');
        };
        $scope.activate = function(mutationModel){
            var req = {};
            req.header = mutationModel;
            req.parameters = [];
            mutationModelService.updateMutationModel(req).then(function() {
            }, function() {
                alertService.error({message:  $.i18n.t('error.common')});
            });
        };
        $scope.doUpdate = function(mutationModel){
            $location.url('/update-mutation-model?id='+mutationModel.id);
        };

    }

    return MutationController;

});