define(['lodash'], function(_) {
    'use strict';

    function MutationInsertController($scope, mutationModelService,alertService,$location,locusService,appConf) {

        $scope.init = function() {
            $scope.showR = false;
            $scope.showr = false;
            $scope.showR2 = false;
            $scope.ignoreSexList = [{"id":1,"description":"SI"},{"id":2,"description":"NO"}];
            $scope.cantSaltosList = [{"id":1,"description":"1"},{"id":2,"description":"2"},{"id":3,"description":"3"},{"id":4,"description":"4"}];
            var parameterGeneric = {};
            parameterGeneric.id = 0;
            parameterGeneric.idMutationModel= 0;
            $scope.parameters = [];
            $scope.defaultMutationRateI = parseFloat(appConf.defaultMutationRateI);
            $scope.defaultMutationRateF = parseFloat(appConf.defaultMutationRateF);
            $scope.defaultMutationRateM = parseFloat(appConf.defaultMutationRateM);
            $scope.defaultMutationRange = parseFloat(appConf.defaultMutationRange);
            $scope.defaultMutationRateMicrovariant = parseFloat(appConf.defaultMutationRateMicrovariant);
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
            locusService.list().then(function(response) {
                mutationModelService.getMutationDefaultParams().then(function(mutationDefaultParams) {

                $scope.locus = response.data.filter(function(l){return l.analysisType===1;});
                $scope.locusById = {};
                $scope.locus.forEach(function(l) {
                    $scope.locusById[l.id] = l;
                });
                $scope.locus.forEach(function (l) {
                    var parameterM =  _.cloneDeep(parameterGeneric);
                    parameterM.locus = l.id;
                    parameterM.sex= 'M';
                    var defaultRVal = _.find(mutationDefaultParams.data,{'locus':parameterM.locus,'sex':parameterM.sex});
                    if(_.isUndefined(defaultRVal)){
                        parameterM.mutationRate = $scope.defaultMutationRateM;
                    }else{
                        parameterM.mutationRate = defaultRVal.mutationRate;
                    }
                    parameterM.mutationRange = $scope.defaultMutationRange;
                    parameterM.mutationRateMicrovariant = $scope.defaultMutationRateMicrovariant;
                    var parameterF = _.cloneDeep(parameterM);
                    parameterF.sex = 'F';
                    defaultRVal = _.find(mutationDefaultParams.data,{'locus':parameterF.locus,'sex':parameterF.sex});
                    if(_.isUndefined(defaultRVal)){
                        parameterF.mutationRate = $scope.defaultMutationRateF;
                    }else{
                        parameterF.mutationRate = defaultRVal.mutationRate;
                    }
                    var parameterI = _.cloneDeep(parameterM);
                    parameterI.sex = 'I';
                    defaultRVal = _.find(mutationDefaultParams.data,{'locus':parameterI.locus,'sex':parameterI.sex});
                    if(_.isUndefined(defaultRVal)){
                        parameterI.mutationRate = $scope.defaultMutationRateI;
                    }else{
                        parameterI.mutationRate = defaultRVal.mutationRate;
                    }
                    $scope.parameters.push(parameterM);
                    $scope.parameters.push(parameterF);
                    $scope.parameters.push(parameterI);
                });
                $scope.parameters = _.orderBy($scope.parameters, ['locus','sex'], ['asc','asc']);

                });
            });

            $scope.selectedMutationModel = {};
            $scope.showParameters = false;
        };

        $scope.init();

        $scope.saveMutationModel = function(){
            var newMutationModelRequest = {};

            var newMutationModel = {};
            newMutationModel.id = 0;
            newMutationModel.mutationType = parseInt($scope.selectedMutationModel.mutationType);
            newMutationModel.name = $scope.selectedMutationModel.name;
            newMutationModel.active = true;
            newMutationModel.ignoreSex = parseInt($scope.selectedMutationModel.ignoreSex)===2;
            newMutationModel.cantSaltos = parseInt($scope.selectedMutationModel.cantSaltos);

            newMutationModelRequest.header = newMutationModel;

            newMutationModelRequest.parameters = [];

            mutationModelService.insertMutationModel(newMutationModelRequest).then(function(response) {
                newMutationModel.id = response.data.id;

                newMutationModelRequest.parameters = $scope.parameters.map(function (parameter) {
                    var mutationType = parseInt($scope.selectedMutationModel.mutationType);
                    if(mutationType === 1){
                        delete parameter.mutationRange ;
                        delete parameter.mutationRateMicrovariant ;
                    }
                    if(mutationType === 2){
                        delete parameter.mutationRateMicrovariant;
                    }
                    delete parameter.$$hashKey;
                    parameter.idMutationModel = newMutationModel.id;
                    return parameter;
                });

                mutationModelService.updateMutationModelByChunks(newMutationModelRequest).then(function() {
                    mutationModelService.generateMatrix(newMutationModelRequest).then(function() {
                        alertService.success({message: 'Fue dado de alta con exito'});
                        $scope.back();
                    }, function() {
                        alertService.error({message: 'Error'});
                    });

                }, function() {
                    alertService.error({message: 'Error'});
                    mutationModelService.deleteMutationModelById(newMutationModel.id).then(function() {
                        alertService.error({message: 'Error'});
                    }, function() {
                        alertService.error({message: 'Error'});
                    });
                });

            }, function() {
                alertService.error({message: 'Error'});
            });

        };
        $scope.back = function(){
            $location.url('/mutation-models');
        };
        $scope.selectMutationType = function(){
            var mutationType = parseInt($scope.selectedMutationModel.mutationType);

            if(mutationType === 1){
                $scope.showR = true;
                $scope.showr = false;
                $scope.showR2 = false;
            }
            if(mutationType === 2){
                $scope.showR = true;
                $scope.showr = true;
                $scope.showR2 = false;
            }
            if(mutationType === 3){
                $scope.showR = true;
                $scope.showr = true;
                $scope.showR2 = true;
            }
            $scope.showParameters = true;

        };
    }

    return MutationInsertController;

});