define(['lodash'], function(_) {
    'use strict';

    function MutationUpdateController($scope, mutationModelService,alertService,$location,$routeParams,locusService,appConf) {

        $scope.init = function() {

            $scope.showR = false;
            $scope.showr = false;
            $scope.showR2 = false;
            $scope.ignoreSexList = [{"id":1,"description":"SI"},{"id":2,"description":"NO"}];
            $scope.cantSaltosList = [{"id":1,"description":"1"},{"id":2,"description":"2"},{"id":3,"description":"3"},{"id":4,"description":"4"}];

            var parameterGeneric = {};
            parameterGeneric.id = 0;
            parameterGeneric.idMutationModel= 0;
            $scope.defaultMutationRate = parseFloat(appConf.defaultMutationRateI);
            // $scope.defaultMutationRateF = parseFloat(appConf.defaultMutationRateF);
            // $scope.defaultMutationRateM = parseFloat(appConf.defaultMutationRateM);
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
            mutationModelService.getMutationModel(parseInt($routeParams.id)).then(function(response) {
                $scope.selectedMutationModel = response.data.header;
                if($scope.selectedMutationModel.ignoreSex){
                    $scope.selectedMutationModel.ignoreSex = _.find($scope.ignoreSexList ,{"id":2}).id;
                }else{
                    $scope.selectedMutationModel.ignoreSex = _.find($scope.ignoreSexList ,{"id":1}).id;
                }

                $scope.parameters = response.data.parameters;
                $scope.parametersByLocus = {};
                $scope.parameters.forEach(function(l) {
                    $scope.parametersByLocus[l.locus] = l;
                });
                $scope.parameters = _.orderBy($scope.parameters, ['locus','sex'], ['asc','asc']);

                $scope.selectMutationType($scope.selectedMutationModel.mutationType);


                locusService.list().then(function(response) {
                    $scope.locus = response.data.filter(function(l){return l.analysisType===1 && !(l.id in $scope.parametersByLocus);});
                    $scope.locusById = {};
                    $scope.locus.forEach(function(l) {
                        $scope.locusById[l.id] = l;
                    });
                    $scope.locus.forEach(function (l) {
                        var parameterM =  _.cloneDeep(parameterGeneric);
                        parameterM.locus = l.id;
                        parameterM.mutationRate = $scope.defaultMutationRate;
                        parameterM.mutationRange = $scope.defaultMutationRange;
                        parameterM.mutationRateMicrovariant = $scope.defaultMutationRateMicrovariant;

                        parameterM.sex= 'M';
                        var parameterF = _.cloneDeep(parameterM);
                        parameterF.sex = 'F';
                        $scope.parameters.push(parameterM);
                        $scope.parameters.push(parameterF);
                    });

                });

            }, function(error) {
                console.log(error);
            });

        };

        $scope.init();

        $scope.saveMutationModel = function(){
            var requestUpdate = {};
            requestUpdate.header = $scope.selectedMutationModel;
            requestUpdate.header.ignoreSex = parseInt(requestUpdate.header.ignoreSex)===2;
            requestUpdate.header.cantSaltos = parseInt(requestUpdate.header.cantSaltos);

            requestUpdate.parameters = $scope.parameters.map(function (parameter) {
                var mutationType = parseInt($scope.selectedMutationModel.mutationType);
                if(mutationType === 1){
                    delete parameter.mutationRange ;
                    delete parameter.mutationRateMicrovariant ;
                }
                if(mutationType === 2){
                    delete parameter.mutationRateMicrovariant;
                }
                delete parameter.$$hashKey;
                return parameter;
            });

            mutationModelService.updateMutationModelByChunks(requestUpdate).then(function() {
                mutationModelService.generateMatrix(requestUpdate).then(function() {
                    alertService.success({message: 'Fue modificado con exito'});
                    $scope.back();
                }, function() {
                    alertService.error({message: 'Error'});
                });
            }, function() {
                alertService.error({message: 'Error'});
            });

        };

        $scope.back = function(){
            $location.url('/mutation-models');
        };
        $scope.selectMutationType = function(mutationType){

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

        };
    }

    return MutationUpdateController;

});