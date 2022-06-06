define([], function() {
	'use strict';
	function ScenarioController($scope, $routeParams, matchesService, profileDataService, scenarioService, laboratoriesService) {
		$scope.profile = $routeParams.p;
        $scope.scenarioId = $routeParams.s;
        $scope.matchingCode = $routeParams.m;
        $scope.isRestricted = $scope.matchingCode !== undefined;
        $scope.preselectedProfiles = $routeParams.ps ? $routeParams.ps : [];

        $scope.profileData = {};
        $scope.tabs = [{active: true}, {active: false}, {active: false}];


        var getProfileData = function(globalCode) {
            return scenarioService.getProfileData(globalCode).then(function(response) {
                $scope.profileData[globalCode] = response.data;
            });
        };

        var getMatchDetails = function() {
            return scenarioService.findMatches($scope.scenarioId, $scope.profile, $scope.matchingCode).then(function(response) {
                $scope.options = response.data;
                $scope.options.forEach(function(m) {
                    m.selectedByP =
                        $scope.scenario.prosecutor.selected.indexOf(m.globalCode) !== -1 ||
                        ($scope.isRestricted && m.globalCode === $scope.matchingCode) ||
                        m.associated;
                    m.selectedByD =
                        $scope.scenario.defense.selected.indexOf(m.globalCode) !== -1 ||
                        m.associated;
                });
                if (!$scope.scenarioId && $scope.preselectedProfiles && $scope.preselectedProfiles.length > 0) {
                    $scope.options = $scope.options.filter(function(ps) {
                       return ps.associated || $scope.preselectedProfiles.indexOf(ps.globalCode) !== -1;
                    });
                } 
                $scope.profiles = $scope.options.map(function(m) {return m.globalCode;});
                matchesService.getComparedMixtureGene($scope.profiles.concat($scope.profile)).then(function(response) {
                    $scope.comparison = response.data;
                });
                $scope.profiles.forEach(function(p){
                    getProfileData(p);
                });
            });
        };

        var loadScenario = function() {
            scenarioService.get({$oid: $scope.scenarioId}).then(function (response) {
                $scope.scenarioData = response.data;
                $scope.result = response.data.result;
                $scope.lastDate = response.data.date.$date;
                $scope.scenario = response.data.calculationScenario;
                $scope.profile = $scope.scenario.sample;
                $scope.isRestricted = response.data.isRestricted;
                getMatchDetails();
                getProfileData($scope.profile);
            });
        };

        var initScenario = function() {
            $scope.scenario = {
                prosecutor: {
                    unknowns: 0,
                    selected: [],
                    unselected: []
                },
                defense: {
                    unknowns: 0,
                    selected: [],
                    unselected: []
                },
                stats: {}
            };
        };

        var getDefaults = function() {
            getProfileData($scope.profile).then(function() {
                laboratoriesService.getLaboratory($scope.profileData[$scope.profile].laboratory).then(function (response) {
                    var laboratory = response.data;
                    $scope.scenario.stats.dropIn = laboratory.dropIn;
                    $scope.scenario.prosecutor.dropOut = laboratory.dropOut;
                    $scope.scenario.defense.dropOut = laboratory.dropOut;
                });
                var defaultSelected = function(att) {
                    return $scope.options.filter(function(o) { return o[att]; }).length;
                };
                $scope.scenario.prosecutor.unknowns = $scope.profileData[$scope.profile].contributors - defaultSelected('selectedByP');
                $scope.scenario.defense.unknowns = $scope.profileData[$scope.profile].contributors - defaultSelected('selectedByD');
            });
        };

        if ($scope.scenarioId){
            loadScenario();
        } else {
            initScenario();
            getMatchDetails().then(function() {
                getDefaults();
            });
        }

        $scope.$watch('scenario', function(newValue, oldValue) {
            if(oldValue) {
                $scope.result = undefined;
            }
        }, true);

        $scope.$watch('options', function(newValue, oldValue) {
            if(oldValue) {
                $scope.result = undefined;
            }
        }, true);

        profileDataService.getCategories().then(function(response){
            $scope.categories = response.data;
        });

        $scope.getSubcatName = function(catId){
            return matchesService.getSubCatName($scope.categories, catId);
        };

        $scope.getLRMix = function(scenario) {
            scenario.isMixMix = false;
            return scenarioService.getLRMix(scenario).then(
                function(response){
                    $scope.result = response.data;
                    $scope.lastDate = new Date();
                    $scope.tabs[2].active = true;
                });
        };


        $scope.fillScenario = function() {
            $scope.scenario.sample = $scope.profile;
            $scope.scenario.prosecutor.selected = getSelection('selectedByP', true);
            $scope.scenario.prosecutor.unselected = getSelection('selectedByP', false);
            $scope.scenario.defense.selected = getSelection('selectedByD', true);
            $scope.scenario.defense.unselected = getSelection('selectedByD', false);
        };

        var getSelection = function(selection, value) {
            return $scope.options.filter(function(m) {
                return m[selection] === value;
            }).map(function (m) {
                return m.globalCode;
            });
        };

	}
	
	return ScenarioController;
});

