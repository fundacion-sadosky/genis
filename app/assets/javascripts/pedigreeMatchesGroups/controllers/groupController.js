define(['lodash','jquery'], function(_,$) {
    'use strict';

    function GroupController($scope, pedigreeMatchesGroupsService, userService, alertService,$location, profiledataService, cryptoService) {
        
        $scope.currentPage = 1;
        $scope.pageSize = 30;

        $scope.sortField = 'date';
        $scope.ascending = false;
        
        $scope.totalItems = '0';

        var user = userService.getUser();
        var userName = user.name;

        $scope.getPedigrees = function(matches) {
//            if ($scope.groupByProfile()) {
                matches.forEach(function (match) {
                    pedigreeMatchesGroupsService.getPedigree(match.pedigree.idPedigree).then(function (response) {
                        match.pedigree.internalSampleCode = response.data.pedigreeMetaData.name;
                        match.pedigree.courtCaseName=response.data.pedigreeMetaData.courtCaseName;
                        match.courtCaseId = response.data.pedigreeMetaData.courtCaseId;
                        match.profile.title= response.data.pedigreeMetaData.title;
                    });
                });
//            }
        };

        var createSearchObject = function(group) {
            var searchObject = {};
            searchObject.kind = group;
            searchObject.sortField = $scope.sortField;
            searchObject.ascending = $scope.ascending;
            searchObject.page = $scope.currentPage - 1;
            searchObject.pageSize = $scope.pageSize;
            searchObject.id = $scope.id;
            searchObject.groupBy = $scope.groupBy;
            searchObject.status = $scope.tab;
            return searchObject;
        };

        var createSearchObjectForReport = function(group) {
            var searchObject = {};
            searchObject.kind = group;
            searchObject.sortField = $scope.sortField;
            searchObject.ascending = $scope.ascending;
            searchObject.page = $scope.currentPage - 1;
            searchObject.pageSize = 500000;
            searchObject.id = $scope.id;
            searchObject.groupBy = $scope.groupBy;
            searchObject.status = $scope.tab;
            return searchObject;
        };

        $scope.searchMatches = function(group) {
          var clase = "tab-estados-tab__" + $scope.status[$scope.filtroAnt].label;
            if(clase !== $scope.clase){
                $("div." + $scope.clase).removeClass($scope.clase + "__selected");
                $scope.clase = clase;
            $scope.tab = $scope.status[$scope.filtroAnt].value;
            $("div." + $scope.clase).toggleClass($scope.clase + "__selected");
            }
            $scope.isProcessing = true;
            var searchObject = createSearchObject(group);
            pedigreeMatchesGroupsService.countMatchesByGroup(searchObject).then(function(response){
                $scope.totalItems = response.headers('X-MATCHES-LENGTH');
               
                if ($scope.totalItems !== '0') {
                    pedigreeMatchesGroupsService.getMatchesByGroup(searchObject).then(function(response) {
                        $scope.matches = response.data.map(function (item) {
                            item.pedigreeMatchResult.internalSampleCode = item.internalCode;
                            return item.pedigreeMatchResult;
                        });

                        if(!$scope.groupByProfile()){
                            profiledataService.getCategories().then(function(response) {
                                $scope.cate = response.data;
                                $scope.categorias = {};
                                _.forEach($scope.cate, function (value) {
                                    _.forEach(value.subcategories,function (key) {
                                        _.forEach($scope.matches, function (matche) {
                                            if (key.id === matche.profile.categoryId) {
                                                matche.profile.categoryId = key.name;
                                            }
                                        });

                                    });

                                });
                            } );
                        }

                        $scope.isProcessing = false;
                        $scope.getPedigrees($scope.matches);
                    }, function() { $scope.isProcessing = false; });
                } else { $scope.isProcessing = false; }
                
            }, function() { $scope.isProcessing = false; });
        };

        $scope.exportarMatches = function () {
            var searchObject = createSearchObjectForReport('Compatibility');
            pedigreeMatchesGroupsService.exportMatchesByGroup(searchObject).then(function () {
                alertService.info({message: $.i18n.t('alerts.profile.exported')});

                 var file_name = "MatchesMPIFile.csv";
                 var url = cryptoService.encryptBase64("/get-pedigreeMatches-export");

                 var a = document.createElement("a");
                 document.body.appendChild(a);
                 a.style = "display: none";
                 a.href = url;
                 a.download = file_name;
                 a.click();
                 document.body.removeChild(a);
            },function (error) {
                alertService.error({message: error.data});
                console.log(error.data);
            });

        };

        $scope.sortBy = function(group, sortField) {
            $scope.currentPage = 1;
            if (sortField !== $scope.sortField) {
                $scope.ascending = true;
            } else {
                $scope.ascending = !$scope.ascending;
            }
            $scope.sortField = sortField;

            $scope.searchMatches(group);
        };

        $scope.discard = function(confirm, jsonMatch){
            if (!confirm) {return;}

            var match = $scope.matches.filter(function(m) { return m._id.$oid === JSON.parse(jsonMatch)._id.$oid; })[0];

            pedigreeMatchesGroupsService.discard(match._id.$oid).then(function(){
                if (userName === match.profile.assignee) {
                    match.profile.status = 'discarded';
                }
                if (userName === match.pedigree.assignee) {
                    match.pedigree.status = 'discarded';
                }
                if (user.superuser) {
                    match.profile.status = 'discarded';
                    match.pedigree.status = 'discarded';
                }
                alertService.success({message: $.i18n.t('alerts.match.discardSuccess')});
                $scope.inicio();
            }, function(response) {
                alertService.error(response.data);
            });
        };
        
        $scope.canDiscard = function(match) {
            return pedigreeMatchesGroupsService.canDiscard(match, user);
        };

        $scope.isHit = function (match) {
            return pedigreeMatchesGroupsService.isHit(match);
        };

        $scope.goToComparison = function(pedigree, profile, oid) {
            return '/#/comparison/' + pedigree + '/matchedProfileId/' + profile + '/matchingId/' + oid + '/pedigreeMatch/true';
        };
        $scope.goToComparisonScreening = function(pedigree, profile, oid) {
            if(oid){
                $location.url('/comparison/' + pedigree + '/matchedProfileId/' + profile + '/matchingId/' + oid + '?isScreening=true');
            }
        };
        $scope.goToScenario = function(courtCaseId, pedigree, profile, matchId) {
            return '/#/pedigree/' + courtCaseId + '/' + pedigree.idPedigree + '?u=' + pedigree.unknown + '&p=' + profile.globalCode + '&m=' + matchId;
        };

        $scope.$on('filtros', function(event,filtro){
            filtros(filtro.filtro);
        });

      function filtros (filtro) {
            var clase = "tab-estados-tab__" + filtro.label;
            if (clase !== $scope.clase) {
                $("div." + $scope.clase).removeClass($scope.clase + "__selected");
                $scope.clase = clase;
            }
            $scope.tab = filtro.value;
            $scope.filtroAnt = filtro.index;
            $("div." + $scope.clase).toggleClass($scope.clase + "__selected");
            $scope.searchMatches("Compatibility");

        }

    }

    return GroupController;

});
