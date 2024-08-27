define(['angular'], function(angular) {
'use strict';

function ScenarioService(playRoutes, $q, $filter, $http) {
	
	this.getLRMix = function(calculationScenario) {
        console.log('LR MIX');
        return  $http.post('/lr-mix', calculationScenario);
        //return playRoutes.controllers.Scenarios.calculateLRMix().post(calculationScenario);
    };

    this.get = function(id) {
        return playRoutes.controllers.Scenarios.get().put(id);
    };

    this.search = function(search) {
        console.log('SCENARIOS SEARCH');
        return  $http.post('/scenarios/search', search);
        //return playRoutes.controllers.Scenarios.search().post(search);
    };

    this.validate = function(scenarioData) {
        console.log('SCENARIOS VALIDATE');
        return  $http.post('/scenarios/validate', scenarioData);
        //return playRoutes.controllers.Scenarios.validate().post(scenarioData);
    };

    this.delete = function(id) {
        return playRoutes.controllers.Scenarios.delete().put(id);
    };
    
    this.findMatches = function(scenarioId, firingCode, matchingCode) {
        return playRoutes.controllers.Scenarios.findMatches(scenarioId, firingCode, matchingCode).get();
    };

    this.createScenario = function(scenario, restricted, name, description, results, geneticist) {
        var data = {
            id: null,
            name: name,
            state: null,
            geneticist: geneticist,
            calculationScenario: scenario,
            isRestricted: restricted,
            result:results,
            description: description
        };
        console.log('SCENARIOS CREATE');
        return  $http.post('/scenarios/create', scenario, restricted, name, description, results, geneticist);
        //return playRoutes.controllers.Scenarios.create().post(data);
    };

    this.update = function(scenario){
        console.log('SCENARIOS UPDATE');
        return  $http.post('/scenarios/update', scenario);
        //return playRoutes.controllers.Scenarios.update().post(scenario);
    };

    this.getProfileData = function(globalCode) {
        var deferred = $q.defer();
        
        $q.all({
            profile: playRoutes.controllers.Profiles.getFullProfile(globalCode).get(),
            profileData: playRoutes.controllers.ProfileData.getByCode(globalCode).get()
        }).then(function(response) {
            response.data = angular.extend(response.profile.data, response.profileData.data);
            deferred.resolve(response);
        });
        
        return deferred.promise;
    };

    var printPlus = function(hypothesis) {
        if (hypothesis.selected.length > 0 && hypothesis.unknowns > 0) {
            return " + ";
        } else {
            return "";
        }
    };

    var printSelected = function(hypothesis, profileData) {
        return hypothesis.selected.map(function (p) {
            if (profileData.hasOwnProperty(p)) {
                return $filter('showcode')(profileData[p]);
            } else {
                return p;
            }
        }).join(" + ");
    };

    var printUnknowns = function(hypothesis) {
        var unknowns = hypothesis.unknowns;
        var unknownsString = "";
        if (unknowns>0) {
            if (unknowns === 1) {
                unknownsString = "1 desconocido";
            } else {
                unknownsString = unknowns + " desconocidos";
            }
        }
        return unknownsString;
    };

    this.printHypothesis = function(hypothesis, profileData) {
        return printSelected(hypothesis, profileData) + printPlus(hypothesis) + printUnknowns(hypothesis);
    };
    
    this.getDefaultScenario = function(firingProfile, matchingProfile, statsOption) {
        console.log('GET DEFAULT SCENARIO');
        return  $http.post('/default-scenario', firingProfile, matchingProfile, statsOption);
        //return playRoutes.controllers.Scenarios.getDefaultScenario(firingProfile, matchingProfile).post(statsOption);
    };
    
    this.getNCorrection = function(firingProfile, matchingProfile, bigN, lr) {
        var correctionRequest = {firingCode: firingProfile, matchingCode: matchingProfile, bigN: bigN, lr: lr};
        console.log('SCENARIOS NCORRECTION');
        return  $http.post('/scenarios/ncorrection', correctionRequest);
        //return playRoutes.controllers.Scenarios.getNCorrection().post(correctionRequest); 
    };
}
	
return ScenarioService
	;

});