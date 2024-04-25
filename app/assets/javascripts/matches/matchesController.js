define(['angular','lodash','jquery'], function(angular,_,$) {
	'use strict';
	function MatchesController($scope, $routeParams , matcherService, profiledataService, alertService,laboratoriesService,profileService, locusService, helper, $anchorScroll, $timeout) {
        $scope.isCollapsing = $routeParams.isCollapsingMatch === "true" || angular.isDefined($scope.courtcaseId);
        console.log('$routeParams', $routeParams);

        $scope.search = {};

        localStorage.removeItem("collapsing");
        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchPedigreeMatches");
        localStorage.removeItem("nuevo");

        $scope.previousSearch = {};
        $scope.pageSize = 30;
        $scope.sortId = 1;
        $scope.sortOptions = [
            {sortField: 'date', ascending: true, label: $.i18n.t('generics.older')},
            {sortField: 'date', ascending: false, label: $.i18n.t('generics.recent')}
        ];
        $scope.matchStatus = matcherService.getMatchStatusEnum();

        $scope.status = [{label: "todos", value: "", index: 0}, {label: $.i18n.t('generics.pendingPlural'), value: "pending", index: 1},
            { label: $.i18n.t('generics.confirmedPlural'),value: "hit", index: 2 },{label: $.i18n.t('generics.discardedPlural'), value: "discarded", index: 3}, {label: $.i18n.t('generics.conflicts'), value: "conflict", index: 4}];

        if(localStorage.length > 0 && localStorage.getItem("searchMatches")){
            var inicio = JSON.parse(localStorage.getItem("searchMatches"));
        var index = $scope.status.filter(function (item) {
           return item.value === inicio.status;
        });

            $scope.currentPage = inicio.page +1;
            $scope.laboratoryCode = inicio.laboratoryCode;
            $scope.categoryCode = inicio.categoria;

            if( index[0] === undefined){
                $scope.tab = $scope.status[0].value;
                $scope.indexFiltro = $scope.status[0].index;
                $scope.clase = "tab-estados-tab__"+$scope.status[0].label;
            }else{
                $scope.tab = index[0].value;
                $scope.indexFiltro = index[0].index;
                $scope.clase = "tab-estados-tab__"+index[0].label;
            }
            $("div." + $scope.clase).toggleClass($scope.clase + "__selected");
        }else{
            $scope.currentPage = 1;
            $scope.tab = $scope.status[1].value;
            $scope.indexFiltro = $scope.status[1].index;
            $scope.clase = "tab-estados-tab__pendientes";
            $("div." + $scope.clase).toggleClass($scope.clase + "__selected");
        }


        $scope.datepickers = {
            hourFrom: false,
            hourUntil: false
        };

        $scope.dateOptions = {
            initDate: new Date()
        };

        locusService.list().then(function(response) {
            var groupedLocus = helper.groupBy(response.data, 'analysisType');
            $scope.locusByAnalysisType = {};
            $.each(groupedLocus, function(key,values) {
                $scope.locusByAnalysisType[key] = values.map(function(l) { return l.id; });
            });
        });

        $scope.toggleDatePicker = function ($event, witch) {
            $event.preventDefault();
            $event.stopPropagation();

            $scope.datepickers[witch] = !$scope.datepickers[witch];
        };

        var createSearchObject = function (search) {
            var searchObject = {};

            if (search.profile && search.profile.length > 0) {
                searchObject.profile = search.profile;
            }
            if (search.courtCaseId) {
                searchObject.courtCaseId = search.courtCaseId;
            }
            if ($scope.courtcaseId) {
                searchObject.courtCaseId = parseInt($scope.courtcaseId);
            }

            if ($scope.tab && $scope.tab.length > 0) {
                searchObject.status = $scope.tab;
            }

            if (search.laboratoryCode && search.laboratoryCode.length > 0) {
                searchObject.laboratoryCode = search.laboratoryCode;
            }

            searchObject.hourFrom = angular.copy(search.hourFrom);


            searchObject.hourUntil = angular.copy(search.hourUntil);
            if (searchObject.hourUntil) {
                searchObject.hourUntil.setHours(23);
                searchObject.hourUntil.setMinutes(59);
                searchObject.hourUntil.setSeconds(59);
            }

            searchObject.page = $scope.currentPage - 1;
            searchObject.pageSize = $scope.pageSize;

            searchObject.categoria = search.category;

            searchObject = angular.extend(searchObject, $scope.sortOptions[$scope.sortId]);

            if(localStorage.length > 0 && localStorage.getItem("searchMatches")){
                searchObject = JSON.parse(localStorage.getItem("searchMatches"));
                $scope.currentPage = searchObject.page +1;
                $scope.pageSize = searchObject.pageSize;
                $scope.search.hourUntil = searchObject.hourUntil;
                $scope.search.hourFrom = searchObject.hourFrom;
                $scope.tab = searchObject.status;
                $scope.courtcaseId= searchObject.courtCaseId;
                $scope.search.profile = searchObject.profile;
                $scope.globalCodeHash = searchObject.globalFocus;
            }


            return searchObject;
        };

        var search = function (filters) {
            $scope.isProcessing = true;
            $scope.noResult = false;
            var searchObject = createSearchObject(filters);
            searchObject.isCollapsing = $scope.isCollapsing;
            $scope.busqueda = searchObject;
            matcherService.getTotalMatches(searchObject).then(function (response) {
                $scope.totalItems = response.headers('X-MATCHES-LENGTH');
                if ($scope.totalItems === '0') {
                    $scope.noResult = true;
                    $scope.isProcessing = false;
                } else {
                    $scope.noResult = false;
                    matcherService.searchMatches(searchObject).then(function (response) {
                        $scope.results = response.data;
                        localStorage.removeItem("searchMatches");
                        _.forEach($scope.results, function (matchOid) {
                          $scope.setTotalMarkers(matchOid);
                            matchOid.matchCard.categoryId=$scope.subCatName(matchOid.matchCard.categoryId);
                            matchOid.matchCardMejorLr.categoryId=$scope.subCatName(matchOid.matchCardMejorLr.categoryId);

                        });
                        if($scope.globalCodeHash !== undefined && !$scope.isCollapsing ){
                            $timeout(function () {
                            $anchorScroll();

                            });
                        }
                        if ($scope.results.length === 0) {
                            $scope.noResult = true;
                        } else {
                            $scope.matches = $scope.results;
                        }
                        $scope.isProcessing = false;
                    }, function () {
                        $scope.isProcessing = false;
                    });
                }
            });
        };

        $scope.searchMatches = function () {
            $scope.previousSearch = angular.copy($scope.search);
            search($scope.search);
        };

        $scope.sort = function () {
            search($scope.previousSearch);
        };
        $scope.discardCollapsing = function (globalCode) {
            matcherService.deleteByLeftProfile(globalCode, parseInt($scope.courtcaseId)).then(function () {
                alertService.success({message: $.i18n.t('alerts.group.discarded')});
                $scope.searchMatches();
            }, function () {
                alertService.error({message: $.i18n.t('alerts.group.discardedError')});
            });
        };
        $scope.clearSearch = function () {
            $scope.search = {};
            $scope.previousSearch = {};
            $scope.sortId = 1;
            $scope.pageSize = 30;
            $scope.currentPage = 1;
            localStorage.removeItem("searchMatches");
            $scope.globalCodeHash = undefined;
            $scope.laboratoryCode = undefined;
            $scope.categoryCode = undefined;
            search({});
        };

        search({});

        profiledataService.getCategories().then(function (response) {
            $scope.cate = response.data;
            $scope.categorias = {};
            $scope.groups = {};
            _.forEach($scope.cate, function (value, key) {
                if (key !== "AM" && key !== "AM_DVI" && key !== "PM" && key !== "PM_DVI") {
                    var n = key;
                    $scope.categorias[n] = value;
                }

            });

            _.forEach($scope.categorias, function (value, key) {
                _.forEach($scope.categorias[key].subcategories, function (value) {
                    $scope.groups[value.name] = value;
                });

            });


        });

        $scope.subCatName= function (catId) {
            var nombre = "";
            _.forEach($scope.categorias, function (value, key) {
                _.forEach($scope.categorias[key].subcategories, function (value) {
                  if(value.id=== catId) {
                      nombre =  value.name;
                  }
                });

            });
        return nombre;
        };

        $scope.getSubCatName = function (catId) {
            return matcherService.getSubCatName($scope.categorias, catId);
        };

        laboratoriesService.getLaboratories().then(function (response) {
            $scope.laboratories = response.data;
        });

        $scope.getGroups = function (m) {
            var filtro = "&i="+$scope.indexFiltro;
            var courtCase = "";
            var isCollapsingMatch = "";
            if ($scope.search.courtCaseId) {
                courtCase = "&idCourtCase=" + $scope.search.courtCaseId;
            } else if ($scope.courtcaseId) {
                courtCase = "&idCourtCase=" + $scope.courtcaseId;
            }
            if ($scope.isCollapsing) {
                isCollapsingMatch = "&isCollapsingMatch=" + $scope.isCollapsing;
            }
            return '#/matchesGroups/groups.html?p=' + m.globalCode + isCollapsingMatch + courtCase + filtro;
        };
        $scope.getGroupsCollapsing = function (m) {
            var courtCase = "";
            var isCollapsingMatch = "";
            if ($scope.search.courtCaseId) {
                courtCase = "&idCourtCase=" + $scope.search.courtCaseId;
            } else if ($scope.courtcaseId) {
                courtCase = "&idCourtCase=" + $scope.courtcaseId;
            }
            if ($scope.isCollapsing) {
                isCollapsingMatch = "&isCollapsingMatch=" + $scope.isCollapsing;
            }
            return '#/court-case/' + $scope.courtcaseId + '?tab=5' + '&p=' + m.globalCode + "&matchesGroup=true" + isCollapsingMatch + courtCase;
        };

        $scope.changePage = function () {
            $scope.searchMatches();
        };

        $scope.maxDate = $scope.maxDate ? null : new Date();
        $scope.minDateCoin = null;

        $scope.checkMaxMin = function (fieldName, fechaMin) {
            var aux = $scope.search[fieldName];
            var min = $scope.search[fechaMin];
            var max = new Date();

            if (min === undefined || min === null) {
                min = $scope.search.hourFrom;
            }
            if (max - aux < 0) {
                alertService.info({message: $.i18n.t('alerts.date.before')});
                $scope.search[fieldName] = undefined;
            } else {
                if (min - aux > 0) {
                    alertService.info({message: $.i18n.t('alerts.date.after')});
                    $scope.search[fieldName] = undefined;
                }
            }

        };

        $scope.checkMax = function (fieldName) {
            var aux = $scope.search[fieldName];
            var today = new Date();

            if (today - aux < 0) {
                alertService.info({message: $.i18n.t('alerts.date.before')});
                $scope.search[fieldName] = undefined;
                $scope.minDateCoin = null;
            } else {
                if (fieldName === 'hourFrom') {
                    if (aux !== undefined || aux !== null) {
                        $scope.minDateCoin = aux;
                    }
                    else {
                        $scope.minDateCoin = null;
                    }
                }
            }

        };

        $scope.descarteMasivo = function (confirm, match) {
            if(!confirm) {return;}

            matcherService.descarteMasivoByGlobalCode(JSON.parse(match).globalCode).then(function () {
                alertService.success({message: $.i18n.t('alerts.group.discarded')});
                $scope.searchMatches();
            }, function(response) {
                if (response.data.message === "Sin matches") {
                    alertService.error({message: $.i18n.t('alerts.match.noMatches')});
                } else {
                    alertService.error({message: $.i18n.t('alerts.match.noMatchesError')});
                }
                $scope.searchMatches();
            });
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


         $scope.setTotalMarkers= function(matches) {
            profileService.getProfile(matches.matchCardMejorLr.globalCode).then(function (response) {
                    var matchMarkers = response.data.genotypification[matches.matchCardMejorLr.typeAnalisis];
                    if (matchMarkers) {
                        matches.matchCardMejorLr.totalMarkers = Object.keys(matchMarkers).filter(function (m) {
                             return $scope.locusByAnalysisType[matches.matchCardMejorLr.typeAnalisis].indexOf(m) !== -1;
                        }).length;
                    }
                }, function (error) {
                    console.log(error);
                    alertService.error({message: error});
                });

        };

         $scope.filtroLocal = function (globalCode) {
             if(!$scope.isCollapsing){
             $scope.busqueda.globalFocus = globalCode;
                localStorage.setItem("searchMatches",JSON.stringify($scope.busqueda));
             }else{
                 localStorage.setItem("collapsing","si");
             }

             };
         
	} return MatchesController;
});

