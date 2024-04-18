define([], function() {
    'use strict';

    function InferiorInstanceController($scope, inferiorInstanceService, alertService) {
        $scope.setConnectivity = function(connectivity,i) {
            $scope.inferiorInstances[i].connectivity = connectivity;
        };

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");
        localStorage.removeItem("nuevo");

        $scope.init = function() {
            $scope.isProcessing = true;

            inferiorInstanceService.getInferiorInstances().then(function(response) {
                $scope.inferiorInstances = response.data;
                //check connections
                for(var i = 0; i < response.data.length; i++) {
                    if(!!response.data[i].url){
                        $scope.checkConnection(response.data[i].url,false);
                    }
                }
            }, function() {
            });

            inferiorInstanceService.getInferiorInstancesStatus().then(function(response) {
                $scope.inferiorInstancesStatus = response.data;

            }, function() {
            });


        };
        $scope.checkConnection = function(url,showMsg) {
            $scope.isProcessing = true;
            return inferiorInstanceService.getConnectionStatus(url).then(function() {
                $scope.isProcessing = false;
                if(showMsg){
                    alertService.success({message: 'Se conecta correctamente'});
                }
                for(var i = 0; i < $scope.inferiorInstances.length; i++) {
                    if($scope.inferiorInstances[i].url===url){
                        $scope.setConnectivity("Operativa",i);
                    }
                }
            }, function(response) {
                $scope.isProcessing = false;
                if(showMsg) {
                    alertService.error({message: response.data.message});
                }
                for(var i = 0; i < $scope.inferiorInstances.length; i++) {
                    if($scope.inferiorInstances[i].url===url){
                        $scope.setConnectivity("No Operativa",i);
                    }
                }
            });
        };
        $scope.getConnectivity = function(url) {
            inferiorInstanceService.getConnectionStatus(url).then(function() {
                return "Operativa";
            }, function() {
                return "No operativa";
            });
        };
        $scope.aprobar = function(id){

            $scope.changeStatus(id,2);

        };
        $scope.desaprobar = function(id){

            $scope.changeStatus(id,3);

        };
        $scope.changeStatus = function(id,idStatus){
            for(var i = 0; i < $scope.inferiorInstances.length; i++){
                if($scope.inferiorInstances[i].id === id){

                    $scope.inferiorInstances[i].idStatus = idStatus;

                    inferiorInstanceService.updateInferiorInstance($scope.inferiorInstances[i]).then($scope.callBackChangeStatusSuccess,$scope.callBackChangeStatusError);

                }
            }

        };
        $scope.getStatusDescription = function(idStatus) {
            if($scope.inferiorInstancesStatus!==undefined){
                for (var i = 0; i < $scope.inferiorInstancesStatus.length; i++) {
                    if ($scope.inferiorInstancesStatus[i].id === idStatus) {
                        return $scope.inferiorInstancesStatus[i].description;
                    }
                }
            }
        };
        $scope.callBackChangeStatusSuccess = function() {

            alertService.success({message: 'Se ha guardado correctamente'});

        };
        $scope.callBackChangeStatusError = function() {

            alertService.error({message: 'No se pudo conectar js'});

        };
        $scope.init();

    }

    return InferiorInstanceController;

});