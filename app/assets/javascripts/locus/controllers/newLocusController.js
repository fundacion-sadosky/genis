define([], function() {
    'use strict';

    function NewLocusController($scope, locusService, alertService) {

        $scope.newLink = {};
        $scope.min= function() {
            if(!$scope.full.locus.minAlleleValue){
                return 0;
            }else{
                return $scope.full.locus.minAlleleValue;
            }
        };
        $scope.max= function() {
            if(!$scope.full.locus.maxAlleleValue){
                return 99;
            }else{
                return $scope.full.locus.maxAlleleValue;
            }
        };
        $scope.initialize = function() {
            $scope.full = {
                locus: {
                    minimumAllelesQty: 1,
                    maximumAllelesQty: 10,
                    contributorsAffected: true,
                    required:true,
                    minAlleleValue:0,
                    maxAlleleValue:99
                },
                alias: [],
                links: []
            };
            $scope.newAlias = undefined;
            $scope.newLink = {};
            if ($scope.locusForm) {
                $scope.locusForm.$setPristine();
            }
        };
        
        $scope.initialize();
        $scope.loadLocus();

        $scope.removeLink = function(index) {
            $scope.full.links.splice(index, 1);
        };
        
        $scope.addLink = function() {
            $scope.full.links.push($scope.newLink);
            $scope.newLink = {};
            $scope.newLinkForm.$setPristine();
        };

        $scope.resetLinks = function() {
            $scope.full.links = [];
            $scope.newLink = {};
            $scope.newLinkForm.$setPristine();
        };
        
        $scope.canBeLinked = function() {
            if($scope.full.locus.analysisType && $scope.analysisTypes) {
                var analysisType = $scope.analysisTypes[$scope.full.locus.analysisType];
                return analysisType.name === 'Autosomal';
            }
        };

        $scope.alreadyAddedLinks = function(item) {
            return $scope.full.links.filter(function(l) { return l.locus === item.locus.id; }).length === 0;
        };

        $scope.save = function() {
            $scope.isProcessing = true;
            if(!$scope.full.locus.chromosome || $scope.full.locus.chromosome==='XY' || $scope.full.locus.chromosome === 'MT'){

                delete $scope.full.locus.minAlleleValue;
                delete $scope.full.locus.maxAlleleValue;

            }

            locusService.add($scope.full).then(function() {
                alertService.success({message: 'Se ha guardado el marcador satisfactoriamente'});
                $scope.initialize();
                $scope.loadLocus();
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

    return NewLocusController;

});
