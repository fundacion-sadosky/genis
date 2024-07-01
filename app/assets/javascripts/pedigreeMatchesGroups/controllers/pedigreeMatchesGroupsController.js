define([], function() {
	'use strict';

	function PedigreeMatchesGroupsController($scope, $routeParams, matchesService, $location, analysisTypeService, pedigreeMatchesGroupsService) {

        $scope.id = $routeParams.p;
        $scope.groupBy = $routeParams.g;

        $scope.matchStatus = matchesService.getMatchStatusEnum();
        $scope.stringency = matchesService.getStrigencyEnum();

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");

        $scope.groupByProfile = function () {
            return $scope.groupBy === 'profile';
        };

        $scope.groupByPedigree = function () {
            return $scope.groupBy === 'pedigree';
        };
        if($routeParams.i !== undefined){
        $scope.filtroAnt = parseInt($routeParams.i);
        }else{
            $scope.filtroAnt = 1;
        }
        $scope.status = [{label: "todos", value: null, index: 0}, {label: "$.i18n.t('generics.pending')", value: "pending", index: 1},
            {label: "$.i18n.t('generics.confirmedPlural')", value: "hit", index: 2}, { label: "$.i18n.t('generics.discarded')",value: "discarded",index: 3}];

        $scope.inicio = function () {

            if ($scope.groupByPedigree()) {
                pedigreeMatchesGroupsService.getPedigreeCoincidencia($scope.id).then(function (response) {
                    var pedigreeData = response.data;
                    $scope.title = pedigreeData.title;
                    $scope.assignee = pedigreeData.assignee;
                    $scope.courtcaseName = pedigreeData.courtCaseName;
                    $scope.caseType = pedigreeData.category;
                    $scope.fecha = pedigreeData.lastMatchDate;
                    $scope.pending = pedigreeData.pending;
                    $scope.hit = pedigreeData.hit;
                    $scope.discarded = pedigreeData.discarded;
                });
            } else {
                pedigreeMatchesGroupsService.findByCodeCoincidencia($scope.id).then(function (response) {
                    var profile = response.data;
                    $scope.title = profile.internalSampleCode;
                    $scope.assignee = profile.assignee;
                    $scope.categoria = profile.category;
                    $scope.fecha = profile.lastMatch;
                    $scope.pending = profile.pending;
                    $scope.hit = profile.hit;
                    $scope.discarded = profile.descarte;
                });
            }
        };

        $scope.inicio();

        analysisTypeService.listById().then(function (response) {
            $scope.analysisTypes = response;
        });

        $scope.getAnalysisName = function (id) {
            if ($scope.analysisTypes) {
                return $scope.analysisTypes[id].name;
            }
        };

        $scope.back = function () {
            $location.url('/pedigreeMatches').hash('foco_' + $scope.id);
        };


        $scope.filtros= function(filtro){
            $scope.$broadcast('filtros',{filtro: filtro});
        };


    }
	return PedigreeMatchesGroupsController;

});
