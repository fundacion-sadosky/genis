define(['angular','lodash','jquery'], function(angular,_,$) {
	'use strict';
	function PedigreeMatchesController($scope, pedigreeMatchesService, matchesService,alertService,profiledataService,$anchorScroll, $timeout) {
        $scope.stringency = matchesService.getStrigencyEnum();
        $scope.isProcessing = false;
        $scope.noResult = true;
        $scope.search = {};
        $scope.previousSearch = {};

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("nuevo");

        $scope.status = [{label: "todos", value: "", index: 0}, {label: "pendientes", value: "pending", index: 1},
            { label: "confirmados",value: "hit", index: 2 },{label: "descartados", value: "discarded", index: 3}];

        if(localStorage.length > 0 && localStorage.getItem("searchPedigreeMatches")){
            var inicio = JSON.parse(localStorage.getItem("searchPedigreeMatches"));
            var index = $scope.status.filter(function (item) {
                return item.value === inicio.status;
            });
            $scope.currentPage = inicio.page +1;
            $scope.categoryCode = inicio.categoria;
            $scope.caseType = inicio.caseType;

            if( index[0] === undefined){
                $scope.tab = $scope.status[0].value;
                $scope.indexFiltro = $scope.status[0].index;
                $scope.clase = "tab-estados-tab__"+$scope.status[0].label;
            }else {
                $scope.tab = index[0].value;
                $scope.indexFiltro = index[0].index;
                $scope.clase = "tab-estados-tab__" + index[0].label;
            }
        }else{
            $scope.currentPage = 1;
            $scope.tab = $scope.status[1].value;
            $scope.indexFiltro = $scope.status[1].index;
            $scope.clase = "tab-estados-tab__pendientes";
            $("div." + $scope.clase).toggleClass($scope.clase + "__selected");
        }

        $scope.matchPStatus = matchesService.getMatchStatusEnum();

        $scope.datepickers = {
            hourFrom : false,
            hourUntil : false
        };

        $scope.dateOptions = {
            initDate : new Date()
        };
        
        $scope.currentPage = 1;
        $scope.pageSize = 30;
        
        $scope.groupId = 'pedigree';

        $scope.toggleDatePicker = function($event, witch) {
            $event.preventDefault();
            $event.stopPropagation();

            $scope.datepickers[witch] = !$scope.datepickers[witch];
        };

        var createSearchObject = function(filters) {
            var searchObject = {};

            if (filters.profile && filters.profile.length > 0 ) {
                searchObject.profile = filters.profile;
            }

            if (filters.category && filters.category.length > 0 ) {
                searchObject.category = filters.category;
            }

            searchObject.caseType = "MPI";

            searchObject.hourFrom = filters.hourFrom;

            searchObject.hourUntil = filters.hourUntil;
            if (searchObject.hourUntil) {
                searchObject.hourUntil.setHours(23);
                searchObject.hourUntil.setMinutes(59);
                searchObject.hourUntil.setSeconds(59);
            }

            if ($scope.tab && $scope.tab.length > 0) {
                searchObject.status = $scope.tab;
            }
            searchObject.page = $scope.currentPage - 1;
            searchObject.pageSize = $scope.pageSize;
            searchObject.group = $scope.groupId;

            if(localStorage.length > 0 && localStorage.getItem("searchPedigreeMatches")){
                searchObject = JSON.parse(localStorage.getItem("searchPedigreeMatches"));
                $scope.currentPage = searchObject.page +1;
                $scope.pageSize = searchObject.pageSize;
                $scope.search.hourUntil = searchObject.hourUntil;
                $scope.search.hourFrom = searchObject.hourFrom;
                $scope.search.category = searchObject.category;
                $scope.tab = searchObject.status;
                $scope.search.profile = searchObject.profile;
                $scope.idHash = searchObject.globalFocus;
                $scope.groupId =  searchObject.group;
                $scope.caseType = searchObject.caseType;
            }




            return searchObject;
        };

        $scope.findMatches = function(filters) {
            $scope.isProcessing = true;
            $scope.noResult = false;
            var searchObject = createSearchObject(filters);
            $scope.busqueda = searchObject;
            pedigreeMatchesService.countMatches(searchObject).then(function(response){
                $scope.totalItems = response.headers('X-MATCHES-LENGTH');

                if ($scope.totalItems !== '0') {
                    pedigreeMatchesService.findMatches(searchObject).then(function(response) {
                        $scope.matches = response.data;
                        localStorage.removeItem("searchPedigreeMatches");
                        if($scope.idHash !== undefined) {
                            $timeout(function () {
                                $anchorScroll();

                            });
                        }
                        $scope.isProcessing = false;
                    }, function() { $scope.isProcessing = false; });
                } else {
                    $scope.noResult = true;
                    $scope.isProcessing = false; 
                }

            }, function() { $scope.isProcessing = false; });
        };

        $scope.clearSearch = function(){
            $scope.search = {};
            localStorage.removeItem("searchPedigreeMatches");
            $scope.categoryCode = undefined;
            $scope.previousSearch = {};
            $scope.idHash = undefined;
            $scope.findMatches($scope.previousSearch);
        };

        $scope.searchMatches = function(){
            $scope.previousSearch = angular.copy($scope.search);
            $scope.findMatches($scope.previousSearch);
        };

        profiledataService.getCategories().then(function(response) {
            $scope.cate = response.data;
            $scope.categorias = {};
            $scope.grupo = {};
            _.forEach($scope.cate, function (value, key) {
                if (key === "AM" || key === "PM" ) {
                    var n = key;
                    $scope.categorias[n] = value;
                }
            });
        });
        $scope.findMatches({});
        
        $scope.searchPrevious = function() {
            $scope.findMatches($scope.previousSearch);
        };

        $scope.getGroups = function(match){
            var filtro = "&i="+$scope.indexFiltro;
            return '#/pedigreeMatchesGroups/groups.html?g=' + match.groupBy + "&p=" + match.id + filtro;
        };

        $scope.maxDate = $scope.maxDate ? null : new Date();
        $scope.minDateCoin = null;

        $scope.checkMaxMin = function(fieldName, fechaMin){
            var aux = $scope.search[fieldName];
            var min = $scope.search[fechaMin];
            var max = new Date();

            if(min === undefined || min === null ) {
                min = $scope.search.hourFrom;
            }
            if(max-aux < 0 ){
                alertService.info({message: 'La fecha debe ser anterior a la actual.'});
                $scope.search[fieldName] = undefined;
            }else{
                if( min-aux > 0 ){
                    alertService.info({message: 'La fecha  debe ser posterior al campo desde.'});
                    $scope.search[fieldName] = undefined;
                }
            }

        };

        $scope.checkMax = function(fieldName) {
            var aux = $scope.search[fieldName];
            var today = new Date();

            if(today-aux < 0 ){
                alertService.info({message: 'La fecha debe ser anterior a la actual.'});
                $scope.search[fieldName] = undefined;
                $scope.minDateCoin = null;
            }else{
                if(fieldName === 'hourFrom' ){
                    if(aux !== undefined || aux !== null) {$scope.minDateCoin = aux; }
                    else { $scope.minDateCoin = null; }
                }
            }

        };

        $scope.filtros = function (filtro) {
            var clase = "tab-estados-tab__" + filtro.label;
            if (clase !== $scope.clase) {
                $("div." + $scope.clase).removeClass($scope.clase + "__selected");
                $scope.clase = clase;
            }
            $scope.tab = filtro.value;
            $scope.indexFiltro = filtro.index;

            $("div." + $scope.clase).toggleClass($scope.clase + "__selected");
            $scope.searchMatches();
        };

        $scope.descarteMasivo = function (confirm, match) {
            if(!confirm) {return;}
            var matchObject = JSON.parse(match);
            pedigreeMatchesService.descarteMasivoByGroup(matchObject.id, matchObject.groupBy).then(function () {
                alertService.success({message: 'Se ha descartado el grupo'});
                $scope.searchMatches();
            }, function(response) {
                if (response.data.message === "Sin matches") {
                    alertService.error({message: 'No hay matches para descartar en el grupo'});
                } else {
                    alertService.error({message: 'Algunos matches del grupo no pudieron ser descartados'});
                    $scope.searchMatches();
                }
            });
        };

        $scope.filtroLocal = function (id) {
            $scope.busqueda.globalFocus = id;
            localStorage.setItem("searchPedigreeMatches",JSON.stringify($scope.busqueda));
        };


    }
	
	return PedigreeMatchesController;
});

