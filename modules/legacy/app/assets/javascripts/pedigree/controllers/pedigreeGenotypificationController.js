define([], function() {
	'use strict';
	function PedigreeGenotypificationController($scope, matchesService,locusService, $routeParams,pedigreeService,$window, alertService) {
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
                    $scope.comparison = response.data;
                });
                verificarPadre();
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
            verificarPadre();
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

            $scope.profilesSearch.pageSize = $scope.pageSize;

            if($scope.search){
                $scope.profilesSearch.searchText = $scope.search;


            $scope.profilesSearch.idCourtCase = $scope.courtCaseId;
            $scope.profilesSearch.statusProfile = "Active";

            pedigreeService.getProfiles($scope.profilesSearch).then(function (response) {
                $scope.isProcessing = false;
                var profileSear = response.data;
                profileSear.forEach(function (x) {
                    if($scope.search === x.globalCode || $scope.search === x.internalCode){
                        var repetido= $scope.selectedOptions.filter(function(elem){return elem.globalCode === x.globalCode;} );
                            if(repetido.length === 0){
                                $scope.show(x);
                            }
                    }
                });
            });
            }

            };

        $scope.searchProfile();

     /*   $scope.$watch('options', function() {
            $scope.selectedOptions = [];
            matchesService.getComparedMixtureGene(Object.keys($scope.options)).then(function (response) {
                $scope.comparison = response.data;
            });
        }, true);
*/


        $scope.marcar= function () {
            $scope.pintar = $scope.agrupador.globalCode;
        };

        $scope.colapsar = function () {
            var agrupadores = "";
            var count = 0;
            if ($scope.selectedOptions.length >= 2) {

                $scope.selectedOptions.forEach(function (elem) {
                    if (elem.groupedBy === elem.globalCode) {
                        agrupadores = agrupadores.concat(elem.globalCode + " ");
                        count ++;
                    }
                });

            if (count > 1) {
                alertService.warning({message: " No se pudo guardar hay mas de un agrupador: " + agrupadores});

            } else {
                var agrupado = [];

                $scope.selectedOptions.forEach(function (elem) {
                    if (elem.groupedBy !== elem.globalCode) {
                        agrupado.push(elem.globalCode.toString());
                    }
                });

                var request = {};
                request.globalCodeParent = $scope.agrupador.globalCode;
                request.globalCodeChildren = agrupado;
                request.courtCaseId = parseInt($scope.courtCaseId);

                pedigreeService.areAssignedToPedigree(request).then(function () {
                    matchesService.confirmSelectedCollapsing($scope.agrupador.globalCode, agrupado, parseInt($scope.courtCaseId)).then(function () {
                        alertService.success({message: 'Se ha confirmado el grupo'});
                        limpiar();
                    }, function () {
                        alertService.error({message: 'Ha ocurrido un error al confirmar el grupo'});
                    });
                }, function (response) {
                    alertService.error({message: response.data.message});
                });


            }
        }else{
                alertService.success({message: 'Debe seleccionar un perfil como agrupador.'});
            }

        };

        
        function verificarPadre() {
            $scope.agrupadorExistente = false;
            $scope.selectedOptions.forEach(function (elem) {
                if(elem.groupedBy=== elem.globalCode){
                    $scope.agrupador= elem;
                    $scope.marcar();
                    $scope.agrupadorExistente = true;
                }
            });
        }

        function limpiar (){
            $scope.agrupador = null;
            $scope.selectedOptions = [];
            $scope.pintar = null;
            $scope.comparison = null;
            $scope.search = null;
        }
        
	}
	
	return PedigreeGenotypificationController;
});

