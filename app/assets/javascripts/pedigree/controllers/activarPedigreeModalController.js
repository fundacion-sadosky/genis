define([], function() {
    'use strict';

function activarPedModalCtrl($scope ,$modalInstance,data,mutationService) {
    $scope.isDVI= data.isDVI;
    $scope.pedigreeActiv= {};
    $scope.pedigreeName= data.pedigreeName;
    $scope.boundary = data.boundary;
    $scope.freqTables = data.freqTables;
    $scope.activeModels = data.activeModels;
    if(data.mutationModelId){
        $scope.mutationModelIdDefault = parseInt(data.mutationModelId);

        $scope.activeModels.forEach(function (item) {
            if(item.id === $scope.mutationModelIdDefault){
                $scope.mutationModelId = item.id;
            }
        });
    }else {
        $scope.mutationModelId = undefined;
    }

    $scope.freqTable=data.frequencyTable;
    $scope.mismatchMito= data.mismatch;
    $scope.mito=data.mito;
    $scope.status= data.status;

    // Si el pedigrí ya tiene un valor propio de "Máximo de exclusiones
    // toleradas" se usa ese; si no, se precarga con el parámetro global
    // configurado en Parámetros Estadísticos (queda editable igual).
    if(data.maxMendelianExclusions !== undefined && data.maxMendelianExclusions !== null){
        $scope.maxMendelianExclusions = parseInt(data.maxMendelianExclusions);
    }else{
        mutationService.getMaxMendelianExclusions().then(function(response) {
            $scope.maxMendelianExclusions = response.data.maxMendelianExclusions;
        });
    }

    $scope.close= function(){
        $scope.pedigreeActiv.boundary = $scope.boundary;
        $scope.pedigreeActiv.frequencyTable = $scope.freqTable;
        $scope.pedigreeActiv.mismatchMito = $scope.mismatchMito;
        $scope.pedigreeActiv.mitocondrial = $scope.mito;
        $scope.pedigreeActiv.maxMendelianExclusions = parseInt($scope.maxMendelianExclusions);
        if($scope.mutationModelId){
            $scope.pedigreeActiv.mutationModelId = $scope.mutationModelId;
        }else {
            delete $scope.pedigreeActiv.mutationModelId;
        }
        $modalInstance.close($scope.pedigreeActiv);
    };


    $scope.isActive = function() {
        return $scope.status === 'Active';
    };

     $scope.onChange = function(freqTable){
         $scope.freqTable = freqTable;
		};

}
    return activarPedModalCtrl;

});