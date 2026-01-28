/**
 * matches groups controllers.
 */
define(['jquery', 'lodash'], function($,_) {
	'use strict';

	function MatchesGroupsController($scope, $routeParams, matchesService, profileService, profileDataService, $filter,
                                     $location, scenarioService, analysisTypeService, locusService, helper, alertService, userService) {
        $scope.isCollapsing = $routeParams.isCollapsingMatch === "true";
		$scope.idCourtCase = parseInt($routeParams.idCourtCase);
		$scope.filtroAnt = parseInt($routeParams.i);
		$scope.matchesDetails = {};
		$scope.globalCode = $routeParams.p;
		$scope.matchStatus = matchesService.getMatchStatusEnum();
		$scope.stringency = matchesService.getStrigencyEnum();
		$scope.pageSize = 30;
        $scope.confirmDisabled = true;
		var initializeStatus = function() {
            $scope.isProcessing = true;
			$scope.normal = {totalItems: '0', currentPage: 1, kind: "Normal", sortField: "date", ascending: false};
			$scope.restricted = {totalItems: '0', currentPage: 1, kind: "Restricted", sortField: "date", ascending: false};
			$scope.mixed = {totalItems: '0', currentPage: 1, kind: "MixMix", sortField: "date", ascending: false};
			$scope.other = {totalItems: '0', currentPage: 1, kind: "Other", sortField: "date", ascending: false};
            $scope.mt = {totalItems: '0', currentPage: 1 , kind: "Mitocondrial",sortField: "date", ascending: false, title : "Mitocondrial"};
		};

        localStorage.removeItem("searchPedigreeMatches");
        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("nuevo");

		initializeStatus();

        $scope.status = [{label: "todos", value: null, index:0}, {label: "pendientes", value: "pending", index:1},
            { label: "confirmados",value: "hit", index:2 },{label: "descartados", value: "discarded", index:3}, {label: "conflictos", value: "conflict", index:4 }];

        locusService.list().then(function(response) {
            var groupedLocus = helper.groupBy(response.data, 'analysisType');
            $scope.locusByAnalysisType = {};
            $.each(groupedLocus, function(key,values) {
                $scope.locusByAnalysisType[key] = values.map(function(l) { return l.id; });
            });
        });

        $scope.backCollapsing = function(){
            localStorage.removeItem("collapsing");
            $location.url('/court-case/'+$scope.courtcaseId+'?tab=5');
        };

        function nuevo() {
            if(localStorage.length >= 0 && !localStorage.getItem("searchMatches") && !localStorage.getItem("collapsing")){
            $scope.backCollapsing();
        }
        }
        nuevo();
        localStorage.removeItem("collapsing");

        analysisTypeService.listById().then(function(response) {
			$scope.analysisTypes = response;
		});
        
        $scope.getAnalysisName = function(id) {
          if ($scope.analysisTypes) {
              return $scope.analysisTypes[id].name;
          }
        };


            function inicio() {
        if($scope.isCollapsing){
            profileService.getProfile($scope.globalCode).then(function (response) {
                $scope.profile = response.data;
                profileDataService.getCategories().then(function (categories) {
               $scope.categories = categories.data;
               $scope.categoryName = $scope.getSubCatName($scope.profile.category.id);
               $scope.categoria = $scope.profile.category.name;
                });
            });

        }else{
            $scope.tab = $scope.status[$scope.filtroAnt].value;
            $scope.clase = "tab-estados-tab__"+$scope.status[$scope.filtroAnt].label;
            $("div." + $scope.clase).toggleClass($scope.clase + "__selected");

        matchesService.searchMatchesProfile($scope.globalCode).then(function(response) {
            var prof = response.data;
            $scope.profile = prof[0];
            profileDataService.getCategories().then(function(categories){
				var categorias = categories.data;
                _.forEach(categorias,function (cate) {
                   _.forEach(cate.subcategories,function (subCate) {
                       if(subCate.id === $scope.profile.categoryId){
                           $scope.profile.isReference = subCate.isReference;
                           $scope.categoria = subCate.name;
                       }
                   });
                });
                $scope.categories = categorias;

			},function(error){
                console.log(error);
                alertService.error({message: error});
            });
			$scope.setGroupsTitles();
		},function(error){
			console.log(error);
            alertService.error({message: error});
        });
        }
    }
        inicio();
        var createSearchObject = function(group) {
            return {
                globalCode: $scope.profile.globalCode,
                internalSampleCode: $scope.profile.internalSampleCode,
                kind: group.kind,
                page: group.currentPage,
                pageSize: $scope.pageSize,
                sortField: group.sortField,
                ascending: group.ascending
            };
        };


		$scope.getMatches = function(group){
			group.isProcessing = true;
            $scope.isProcessing = true;
            var searchObject = createSearchObject(group);
            searchObject.isCollapsing = $scope.isCollapsing;
            searchObject.courtCaseId = $scope.idCourtCase;
            if(!$scope.isCollapsing)
            { searchObject.status = $scope.tab; }

            if(group.kind === "Mitocondrial"){
            searchObject.tipo = 4;
            }
            matchesService.getTotalMatchesByGroup(searchObject).then(function(response){
                group.totalItems = response.headers('X-MATCHES-GROUP-LENGTH');
                if (group.totalItems !== '0') {
                    matchesService.getMatchesByGroup(searchObject).then(function(response){
                        console.log(response.data);
                        group.matches = response.data;
                        $scope.setTotalMarkers(group.matches);
                        $scope.setQuantities();
                        $scope.setGroupsTitles();
                        group.isProcessing = false;
                        $scope.isProcessing = false;
                    },function(error){
                        console.log(error);
                        alertService.error({message: error});
                    });
                } else {
                    group.isProcessing = false;
                    $scope.isProcessing = false;
                }
            },function(error){
                console.log(error);
                alertService.error({message: error});
            });
		};

        $scope.setQuantities = function() {
			if (userService.hasPermission("SCENARIO_CRUD")) {
                if ($scope.normal.matches) {
                    scenarioService.search({profile: $scope.globalCode, ascending: false, sortField: 'date'}).then(function (response) {
                        $scope.normal.viewScenarios = response.data.length > 0;
                    });
                }
                if ($scope.restricted.matches) {
                    $scope.restricted.matches.forEach(function (m) {
                        scenarioService.search({profile: m.globalCode, ascending: false, sortField: 'date'}).then(function (response) {
                            m.viewScenarios = response.data.length > 0;
                        }, function (error) {
                            console.log(error);
                        });
                    });
                }
            }
        };

		$scope.setTotalMarkers = function(matches){
			matches.forEach(function(match){
				profileService.getProfile(match.globalCode).then(function(response) {
					var matchMarkers = response.data.genotypification[match.type];
					if (matchMarkers) {
						match.totalMarkers = Object.keys(matchMarkers).filter(function(m) { return $scope.locusByAnalysisType[match.type].indexOf(m) !== -1; }).length;
					}
				},function(error){
                    console.log(error);
                    alertService.error({message: error});
                });
			});
        };
		
		$scope.getLR = function(m) {
			m.isProcessing = true;
			return matchesService.getLR($scope.profile.globalCode, m.globalCode, m.oid, undefined).then(function (r) {
				m.lrs = r.data;
				m.isProcessing = false;
			}, function() {
				alertService.info({message: 'No existen parámetros para el cálculo del LR'});
				m.isProcessing = false;
			});
		};

		$scope.getAllLR = function(category) {
            category.matches.forEach(function(match) {
                if ($scope.isCalculable(match)) {
                    $scope.getLR(match);
                }
            });
		};

        $scope.discardCollapsingGroup = function() {
            matchesService.deleteByLeftProfile($scope.globalCode,$scope.idCourtCase).then(function () {
                alertService.success({message: 'Se ha descartado el grupo'});
                $scope.backCollapsing();
            }, function() {
                alertService.error({message: 'Ha ocurrido un error al descartar el grupo'});
            });
        };
        $scope.confirmSelectedCollapsing = function() {
            var globalCodesChilds = $scope.normal.matches.filter(function (element) {
                return element.selected;
            }).map(function (element) {
                return element.globalCode;
            });
            matchesService.confirmSelectedCollapsing($scope.globalCode,globalCodesChilds,$scope.idCourtCase).then(function () {
                alertService.success({message: 'Se ha confirmado el grupo'});
                $scope.confirmDisabled = true;
                $scope.getMatches($scope.normal);
            }, function(error) {
                console.log(error);
                alertService.error({message: error.data});
            });
        };

        $scope.isCalculable = function(match) {
          if ($scope.analysisTypes) {
              return $scope.analysisTypes[match.type].name === 'Autosomal';
          }  
        };

		$scope.setGroupsTitles = function(){
			if ($scope.profile.isReference){
				$scope.normal.title = "Referencias";
				$scope.restricted.title = "Evidencias";
			} else {
				if ($scope.profile.contributors === 1){
					$scope.normal.title = "Referencias o Evidencias Cantidad de Aportantes Inferidos = 1";
					$scope.restricted.title = "Evidencias Cantidad de Aportantes Inferidos > 1";
				} else if ($scope.profile.contributors === 2){
					$scope.normal.title = "Referencias o Evidencias Cantidad de Aportantes Inferidos = 1";
					$scope.mixed.title = "Evidencias Cantidad de Aportantes Inferidos = 2";
					$scope.other.title = "Evidencias Cantidad de Aportantes Inferidos > 2";
				} else {
					$scope.normal.title = "Referencias o Evidencias Cantidad de Aportantes Inferidos = 1";
					$scope.other.title = "Evidencias Cantidad de Aportantes Inferidos > 1";
				}
			}
		};

		$scope.getSubCatName = function(id){
			return matchesService.getSubCatName($scope.categories, id);
		};

		$scope.getHref = function(m, d) {
			return '/#/comparison/' + m.globalCode + '/matchedProfileId/' + d.globalCode + '/matchingId/' + d.oid + /collapsingMatch/ + $scope.isCollapsing ;
		};

		$scope.getScenarios = function(match) {
			if (!match) {
				$location.url('/scenarios/scenarios.html').search({ p:$scope.globalCode });
			}else{
				$location.url('/scenarios/scenarios.html').search({ p:match.globalCode });
			}
		};

		$scope.addScenario = function(match){
			if (!match && $scope.normal.matches){
				var preselectedProfiles = $scope.normal.matches.reduce(function(array, match){
					if (match.selected) {
						array.push(match.globalCode);
					}
					return array;
				}, []);

				if (preselectedProfiles.length > 0){
					$location.url('/scenarios/scenario.html').search({ p:$scope.globalCode, ps:preselectedProfiles });
				}else{
					$location.url('/scenarios/scenario.html').search({ p:$scope.globalCode });
				}
			} else {
				$location.url('/scenarios/scenario.html').search({ p:match.globalCode, m:$scope.globalCode });
			}
		};

		$scope.discardMatch = function(confirm, match){
			if (!confirm) {return;}

			matchesService.doDiscard(JSON.parse(match).oid, $scope.globalCode).then(function(){
				initializeStatus();
                inicio();
                $scope.filtros($scope.status[$scope.filtroAnt]);

            });
		};

        $scope.uploadMatch = function(confirm, match){
            if (!confirm) {return;}

            matchesService.doUpload(JSON.parse(match).oid, $scope.globalCode).then(function(){
                initializeStatus();
                inicio();
                $scope.filtros($scope.status[$scope.filtroAnt]);

            });
        };

        $scope.canUpload = function(match){
            return matchesService.canUpload(match.oid);
        };

		$scope.changeSelection = function(value) {
			$scope.normal.matches.forEach(function(m) {
				if($scope.isCalculable(m)) {
                    m.selected = value;
                }
			});
		};
        $scope.checkActivation = function() {
            $scope.confirmDisabled = !$scope.isAnySelected();
        };
        $scope.isAnySelected = function() {
            for (var i = 0; i < $scope.normal.matches.length; i++) {
                if($scope.normal.matches[i].selected) {
                    return true;
                }
            }
            return false;
        };
		$scope.back = function(){
			$location.url('/matches/').hash('foco_'+$scope.globalCode);
		};



			$scope.sortBy = function(group, sortField) {
			if (sortField !== group.predicate) {
				group.reverse = false;
			} else {
				group.reverse=!group.reverse;
			}
			group.predicate = sortField;

            group.sortField = sortField;
            group.currentPage = 1;
            group.ascending = !group.reverse;
            $scope.getMatches(group);
		};

        $scope.chunks = function(array, groupQuantity) {
            var result = [],
                index = 0,
                size = array && array.length ? array.length : 0;

            while (index < size) {
                result.push(array.slice(index, index += groupQuantity));
            }

            return result;
        };

        $scope.filtros = function (filtro) {
            var clase = "tab-estados-tab__" + filtro.label;
            if (clase !== $scope.clase) {
                $("div." + $scope.clase).removeClass($scope.clase + "__selected");
                $scope.clase = clase;
            }
            $scope.tab = filtro.value;
            $scope.filtroAnt = filtro.index;
            $("div." + $scope.clase).toggleClass($scope.clase + "__selected");
            $scope.getMatches($scope.normal);
            $scope.getMatches($scope.restricted);
            $scope.getMatches($scope.mixed);
            $scope.getMatches($scope.other);
            $scope.getMatches($scope.mt);

        };

        $scope.descarteMasivo = function (confirm, group) {
            if (!confirm){ return; }
            var matchesToDiscard = JSON.parse(group).matches.filter(function (m) {return m.ownerStatus !== 'discarded';});
            
            var matchesOid = matchesToDiscard.map(function(m){return m.oid;});
            matchesService.descarteMasivoByList($scope.globalCode, matchesOid).then(function () {
                alertService.success({message: 'Se ha descartado el grupo'});
                initializeStatus();
                inicio();
                $scope.filtros($scope.status[$scope.filtroAnt]);
            }, function(response) {
                if (response.data.message === "Sin matches") {
                    alertService.error({message: 'No hay matches para descartar en el grupo'});
                } else {
                    alertService.error({message: 'Algunos matches del grupo no pudieron ser descartados'});
                    initializeStatus();
                    inicio();
                    $scope.filtros($scope.status[$scope.filtroAnt]);
                }
            });

        };

    }

	return MatchesGroupsController;

});
