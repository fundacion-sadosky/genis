define(['lodash'], function(_) {
    'use strict';
    function PedigreeComparisonController(
        $scope,
        matchesService,
        locusService,
        $routeParams,
        profileService,
        $window,
        alertService,
        searchService
    ) {
        $scope.shouldShowMaxAlelle = locusService.shouldShowMaxAlelle;
        $scope.shouldShowMinAlelle = locusService.shouldShowMinAlelle;
        $scope.showDifferences = false;
        $scope.showMatches = false;
        $scope.selectedOptions = [];
        $scope.courtCaseId = parseInt($routeParams.courtCaseId);
        $scope.profilesSearch = {};
        $scope.pageNumber = 1;
        $scope.pageSize = 10;
        $scope.profilesSearch.page = 0;
        $scope.agrupadorExistente = false;

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        locusService.listFull().then(function (response) {
            $scope.listFull= response.data;
            $scope.locusById = _.keyBy(_.map($scope.listFull, function (o) {
                return o.locus;
            }), 'id');
        });

        locusService.listRanges().then(function (response) {
            $scope.allelesRanges= response.data;
        });

        $scope.availableOptions = function() {
            var available = [];
            for(var gc in $scope.options) {
                if ($scope.options.hasOwnProperty(gc) && $scope.selectedOptions.indexOf($scope.options[gc]) === -1) {
                    available.push($scope.options[gc]);
                }
            }
            return available;
        };

        $scope.show = function(option) {
            if (option && ($scope.selectedOptions.length < 5)) {
                $scope.selectedOptions.push(option);
                var lista = $scope.selectedOptions.map(function(element){
                    return element.globalCode;
                });
                matchesService.getComparedMixtureGene(lista).then(function (response) {
                    $scope.comparison  = response.data.filter(function (item) {
                        return $scope.locusById[item.locus] && $scope.locusById[item.locus].analysisType !== 4;
                    });
                });
            }
        };

        $scope.hide = function(option) {
            if (option) {
                var index = $scope.selectedOptions.indexOf(option);
                if (index > -1) {
                    if($scope.pintar !== undefined && option.globalCode === $scope.pintar){
                        $scope.pintar = null;
                    }
                    $scope.selectedOptions.splice(index, 1);
                }
            }
        };

        $scope.displayDifferences = function() {
            $scope.showMatches = false;
            $scope.showDifferences = !$scope.showDifferences;
        };

        $scope.displayMatches = function() {
            $scope.showDifferences = false;
            $scope.showMatches = !$scope.showMatches;
        };

        $scope.hasMatches = function(g, allele) {
            var result = true;
            $scope.selectedOptions.forEach(function(individual) {
                var alleles = g[individual.globalCode];

                if (alleles.indexOf(allele) === -1) {
                    result = false;
                }
            });
            return result;
        };

        $scope.searchProfile= function(){
            if($scope.selectedOptions.length ===2){
                alertService.error({'message': 'Solo se pueden comparar hasta 2 perfiles'});
                return;
            }
            $scope.profilesSearch.pageSize = $scope.pageSize;
            $scope.searchObj = {
                input: $scope.search,
                active: true,
                inactive: false,
                page:0,
                pageSize:2,
                notUploaded: null,
                category: ""
            };
            if($scope.search){
                searchService
                    .search($scope.searchObj)
                    .then(function (response) {
                        $scope.isProcessing = false;
                        if(response.data.length>0){
                            var first = _.find(
                                response.data,
                                function(o) {
                                    return o.internalSampleCode === $scope.search || o.globalCode === $scope.search;
                                });
                            if(first){
                                var r = {};
                                r.internalCode = first.internalSampleCode;
                                r.globalCode = first.globalCode;
                                var existing = _.find(
                                    $scope.selectedOptions,
                                    function(o) {
                                        return o.internalCode === r.internalCode || o.globalCode === r.globalCode;
                                    });
                                if(!existing){
                                    $scope.show(r);
                                }
                            }else{
                                alertService.error({'message': 'Perfil no encontrado'});
                            }
                        }else{
                            alertService.error({'message': 'Perfil no encontrado'});
                        }
                    },function () {
                        alertService.error({'message': 'Perfil no encontrado'});
                    });
            }

        };

        $scope.searchProfile();



    }

    return PedigreeComparisonController;
});