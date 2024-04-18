define([], function() {
    'use strict';

    function NewKitController($scope, analysisTypeService, kitService, alertService, locusService) {

        $scope.initialize = function() {
            $scope.kit = {
                locus: [],
                alias: []
            };
            $scope.newAlias = undefined;
            $scope.newLocus = {};
            if ($scope.kitForm) {
                $scope.kitForm.$setPristine();
            }
        };
        
        $scope.initialize();
        $scope.fluorophores = locusService.getFluorophoreEnum();

        locusService.list().then(function(response) {
            $scope.locus = response.data;
            $scope.locusById = {};
            response.data.forEach(function(l) {
                $scope.locusById[l.id] = l;
            });
        });
        
        analysisTypeService.list().then(function(response) {
            $scope.analysisTypes = response.data;
        });
        
        $scope.alreadyAdded = function(item) {
            return $scope.kit.locus.filter(function(l) { return l.locus === item.id; }).length === 0;
        };

        $scope.addLocus = function() {
            $scope.kit.locus.push($scope.newLocus);
            $scope.newLocus = {};
        };

        $scope.removeLocus = function(index) {
            $scope.kit.locus.splice(index, 1);
        };

        $scope.disableAddLocus = function(collection, item) {
            return !item || item.length === 0;
        };

        $scope.save = function() {
            $scope.isProcessing = true;
            $scope.kit.locy_quantity = $scope.kit.locus.length;
            for (var i = 0; i < $scope.kit.locus.length; i++) {
                $scope.kit.locus[i].order = i + 1;
            }
            kitService.add($scope.kit).then(function() {
                alertService.success({message: 'Se ha guardado el kit satisfactoriamente'});
                $scope.initialize();
                $scope.loadKits();
                $scope.isProcessing = false;
            }, function(response) {
                alertService.error(response.data);
                $scope.isProcessing = false;
            });
        };

        $scope.cancel = function() {
            $scope.initialize();
        };

    }

    return NewKitController;

});