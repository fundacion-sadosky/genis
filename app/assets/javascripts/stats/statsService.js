define(['jquery'], function($) {
	'use strict';

function StatsService(playRoutes, $q) {
					
	this.getBaseByName = function(name) {
		return playRoutes.controllers.PopulationBaseFreq.getByName(name).get();
	};
	
	this.setAsDefault = function(name){
		return playRoutes.controllers.PopulationBaseFreq.setBaseAsDefault(name).put();
	};
	
	this.toggleStateBase = function(name) {
		return playRoutes.controllers.PopulationBaseFreq.toggleStateBase(name).put();
	};

	this.getAllBasesNames = function() {
		return playRoutes.controllers.PopulationBaseFreq.getAllBaseNames().get();
	};
	
	this.getAllBasesCharacteristics = function() {
		return playRoutes.controllers.PopulationBaseFreq.getAllBasesCharacteristics().get();
	};
		
	this.getActiveTables = function(){
		return this.getAllBasesCharacteristics().then(function(response) {
			var tables = response.data;
			var ret = {};
			$.each(tables, function(tName, tChars) { 
				if (tChars.state){
					tChars.a = false;
					ret[tName] = tChars;
				}
			});
			return ret;
		});
	};
	
	function getDefault(freqTables){
		var defaultOptions = {};
		defaultOptions = {frequencyTable: undefined, probabilityModel: undefined, theta: undefined};
		Object.keys(freqTables).forEach(function(dbName) {
			if (freqTables[dbName].default) { 
				defaultOptions.frequencyTable = dbName;
				defaultOptions.theta = freqTables[dbName].theta;
				defaultOptions.probabilityModel = freqTables[dbName].model;
			}
		});
		return defaultOptions;
	}
	
	this.getDefaultDB = function() {
		return this.getActiveTables().then(function(tables) {
			return getDefault(tables);			
		});
	};

    this.getDefaultOptions = function(globalCode) {
        
        var promises = [];

        promises.push(this.getDefaultDB());

        var aux = $q.defer();
        playRoutes.controllers.ProfileData.getByCode(globalCode).get().then(function(response) {
           var laboratory = response.data.laboratory;
           playRoutes.controllers.Laboratories.getLaboratory(laboratory).get().then(function(r) {
               aux.resolve(r.data);
           }, function(error) { aux.reject(error); });
        }, function(error) { aux.reject(error); });

        promises.push(aux.promise);

        var deferred = $q.defer();
         $q.all(promises).then(
         function(response){
            var options = 
                { 'frequencyTable': response[0].frequencyTable, 'theta': response[0].theta, 'probabilityModel': response[0].probabilityModel,
                    'dropIn': response[1].dropIn, 'dropOut': response[1].dropOut };
            if (!options.frequencyTable || !options.probabilityModel || options.theta === undefined) {
                 deferred.reject($.i18n.t('alerts.generics.dbNotConfigured'));
			} else if (options.dropIn === undefined || options.dropOut === undefined) {
                options.dropIn = 0.0;
                options.dropOut = 0.0;
                deferred.resolve(options);
                //deferred.reject('No están configurados drop in y drop out del laboratorio.');
			} else {
                 deferred.resolve(options);
			}
         },
         function(response){
            deferred.reject(response);
         }
         );

         return deferred.promise;
    };
	
	this.getDefaultDBOption = function(tables) {
		return getDefault(tables);	
	};
	
	this.getStatModels = function(){
		return [{id: 'HardyWeinberg',name: 'Hardy-Weinberg', enableTheta: false, show: true}, 
                {id: 'NRCII41',name: 'NRC II Recommendation 4.1', enableTheta: true, show: true}, 
                {id: 'NRCII410',name: 'NRC II Recommendation 4.10', enableTheta: true, show: true}];
	};
	
	this.getFminCalcOptions = function(){
		return {'FminValue': {id: 'value', description: 'Valor', config: ['fmin']}, 
				'NRCII': {id: 'NRCII', description: 'NRC II', config: ['N']}, 
				'Weir': {id: 'Weir', description: 'Weir', config: ['N', 'alpha']},
				'BudowleMonsonChakraborty': {id: 'BudowleMonsonChakraborty', description: 'Budowle, Monson, Chakraborty', config: ['N', 'alpha', 'C']}};
	};
	
	this.insertFmin = function(cacheId, fmin) {
		var request = {calcOption: fmin.calcOption, config: {}};	
		$.each(fmin.config, function(locus, config){
			request.config[locus] = Object.keys(config).map(function(p){return parseFloat(config[p]);});
		});		
		return playRoutes.controllers.PopulationBaseFreq.insertFmin(cacheId).put(request);
	};
	
}

	return StatsService;
});