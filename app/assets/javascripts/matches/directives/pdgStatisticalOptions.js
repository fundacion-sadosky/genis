define([], function() {
'use strict';

function pdgStatisticalOptions() {

	return {
		restrict : 'E',
		scope: {
			header: "=",
			panel: "=",
			process: "=",
			selectedOptions: "=",
			processBase: "&processFuncion",
			onLoadBase: "&onLoad",
			noDefault: "=",
			freqTables: "=",
			mix: "="
		},
		templateUrl: '/assets/javascripts/matches/directives/pdg-statistical-options.html',
		
		link : function(scope) {
			scope.thetaRe = new RegExp("^0\\.?[0-9]{0,3}$|^1$");
			scope._header = (scope.header !== undefined)? (scope.header.toString().toLowerCase() === 'true') : true;
			scope._panel = (scope.panel !== undefined)? (scope.panel.toString().toLowerCase() === 'true') : true;
			scope._process = (scope.process !== undefined)? (scope.process.toString().toLowerCase() === 'true') : true;
			
			scope.$watch('selectedOptions.frequencyTable', function(db) {
				if (db && db !== null && scope.freqTables && scope.freqTables[db]) {
					scope.selectedOptions.theta = scope.freqTables[db].theta;
					scope.selectedOptions.probabilityModel = scope.freqTables[db].model;
				}
			}, true);
			
			scope.getRandomMatchProbabilities = function() {
				scope.processBase()(scope.selectedOptions.frequencyTable, scope.selectedOptions.probabilityModel, scope.selectedOptions.theta);
			};
			
		}
	};
}

return pdgStatisticalOptions;

});