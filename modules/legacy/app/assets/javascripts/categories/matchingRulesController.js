define(['angular'], function(ng) {
'use strict';

function MatchingRulesController ($scope, appConf, $filter) {
	
	$scope.stringencies = appConf.stringencies;
	$scope.matchingAlgorithms = appConf.matchingAlgorithms;

    $scope.isMT = function() {
        return $scope.activeAnalysis && $scope.analysisTypes[$scope.activeAnalysis].mitochondrial;
    };
    
    $scope.newMatchingRule = function(){
        $scope.newMatchingRules = { considerForN: true };
        if ($scope.isMT()){
            $scope.newMatchingRules.matchingAlgorithm = 'ENFSI';
            $scope.newMatchingRules.minimumStringency = 'HighStringency';
            $scope.findStringencyOption($scope.newMatchingRules);
            $scope.newMatchingRules.minLocusMatch = 11; //no se usa
            $scope.newMatchingRules.considerForN = false; //no se usa
        }
        if ($scope.newMatchingRuleForm) {
            $scope.newMatchingRuleForm.$setPristine();
        }
    };

    $scope.newMatchingRule();

    $scope.stringencyOptions = [];

    $scope.showStringencies = function(input){
        return !(input === 'NoMatch' || input === 'Mismatch');
    };

    $scope.stringencies.forEach(function(s) {
        if ($scope.showStringencies(s)) {
            $scope.stringencyOptions.push({matchingAlgorithm: 'ENFSI', minimumStringency: s});
        }
    });

    $scope.stringencyOptions.push({matchingAlgorithm: 'GENIS_MM', minimumStringency: 'ModerateStringency'});
    
    $scope.changeStringency = function(obj, option) {
        obj.matchingAlgorithm = $scope.stringencyOptions[option].matchingAlgorithm;
        obj.minimumStringency = $scope.stringencyOptions[option].minimumStringency;
    };
    
    $scope.printStringency = function(option) {
        if (option.matchingAlgorithm === 'ENFSI') {
            return $filter('i18next')('stringency.' + option.minimumStringency);
        } else {
            return $filter('i18next')('matchingAlgorithm.' + option.matchingAlgorithm);
        }
    };
	
	$scope.removeAssociation = function(index) {
		var category = $scope.categories[$scope.currCatId];
		category.matchingRules.splice(index, 1);
	};

	$scope.canRemoveMatchingRule = function(matchingCategory){
        var category = $scope.categories[$scope.currCatId];
        var categoryRelated = $scope.categories[matchingCategory];
        return !category.pedigreeAssociation || !categoryRelated.pedigreeAssociation;
    };
	
	$scope.addAssociation = function() {
		var category = $scope.categories[$scope.currCatId];
        category.matchingRules.push(
            ng.extend({failOnMatch: false, forwardToUpper: false, type: $scope.activeAnalysis}, 
                    $scope.newMatchingRules));
		$scope.newMatchingRule();
	};
    
    $scope.canAddAssociation = function() {
        if ($scope.categories && $scope.currCatId) {
            var completeFields = $scope.newMatchingRules.mismatchsAllowed !== undefined &&
                $scope.newMatchingRules.minLocusMatch !== undefined &&
                $scope.newMatchingRules.mismatchsAllowed < $scope.newMatchingRules.minLocusMatch &&
                $scope.newMatchingRules.categoryRelated !== undefined &&
                $scope.newMatchingRules.matchingAlgorithm !== undefined;
            var uniqueAlgorithm = $scope.categories[$scope.currCatId].matchingRules.filter(function (mr) {
                    return mr.categoryRelated === $scope.newMatchingRules.categoryRelated && mr.matchingAlgorithm === $scope.newMatchingRules.matchingAlgorithm && mr.type === $scope.activeAnalysis;
                }).length === 0;
            return completeFields && uniqueAlgorithm;
        }

        return false;
    };

    $scope.findStringencyOption = function(conf) {
        $scope.stringencyOptions.forEach(function(o, $index) {
            if (o.matchingAlgorithm === conf.matchingAlgorithm && o.minimumStringency === conf.minimumStringency) {
                conf.stringencyOption = $index;
            }
        });
    };
	
	$scope.validateMatchingRule = function(conf, field) {
        if (conf.mismatchsAllowed === null || conf.minLocusMatch === null || conf.mismatchsAllowed >= conf.minLocusMatch) {
            if (field === 'mismatchsAllowed') {
                conf.mismatchsAllowed = conf.minLocusMatch - 1;
            }
            if (field === 'minLocusMatch') {
                conf.minLocusMatch = conf.mismatchsAllowed + 1;
            }
        }
	};

	$scope.$on('form-reset', function(){
		$scope.newMatchingRule();
	});

    $scope.filterByAnalysisType = function(stringencyOption){
        if ($scope.activeAnalysis && stringencyOption.matchingAlgorithm === 'GENIS_MM') {
            if ($scope.analysisTypes[$scope.activeAnalysis].name === "Cx" || $scope.analysisTypes[$scope.activeAnalysis].name === "Cy"){
                return false;
            }
        }

        return true;
    };
}

return MatchingRulesController;

});