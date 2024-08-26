define(['jquery'], function(jquery) {
'use strict';

	function HypothesesService() {
		
		var postfixDView = 'Desconocido_H2_'; // Cuando se haga la internalización, reemplazar esta constante
		var postfixPView = 'Desconocido_H1_'; // Cuando se haga la internalización, reemplazar esta constante
		var postfixD = '?D';
		var postfixP = '?P';
		
		function getHypoteses(profiles, contributors, posfix) { // Posibles hipotesis
			
			var setOptions = profiles.reverse();
			
			var sets = (function(input, size){
				var i;
				var results = [], result, mask, total = Math.pow(2, input.length);
				for(mask = 0; mask < total; mask++){
					result = [];
					i = input.length - 1; 
					do{
						if( (mask & (1 << i)) !== 0){
							result.push(input[i]);
						}
					} while(i--);
					if( result.length >= size){
						results.push(result);
					}
				}

				return results; 
			})(setOptions, 1);
		
			var setsRet = sets.map(function(item){
				var aux = [].concat(item);
				var n = 1;
				for(var i = item.length; i < contributors; i++){
					aux.push(posfix + n++);
				}
				return aux;
			});
		
			var allUnknown = [];
			for(var i = 1; i<= contributors; i++){
				allUnknown.push(posfix + i);
			}
			setsRet.push(allUnknown);
			
			return setsRet;
		}
		
		
		this.getDefenderHypoteses = function(profiles, contributors) {
			return getHypoteses(profiles, contributors, postfixDView);
		};
		
		this.getProsecutorHypoteses = function(profiles, contributors) {
			return getHypoteses(profiles, contributors, postfixPView);
		};
		
		this.createStatsOptions = function(profiles, selectedHypotesisProsecutor, selectedHypotesisDefender){
			var aux = [].concat(profiles);
			
			if (selectedHypotesisProsecutor) { 
				selectedHypotesisProsecutor.forEach(function(x){
					if (aux.indexOf(x) === -1) { aux.push(x); }
				});
			}
			
			if (selectedHypotesisDefender) { 
				selectedHypotesisDefender.forEach(function(x){
					if (aux.indexOf(x) === -1) { aux.push(x); }
				});
			}
			
			return aux;
		};
		
		this.setStatsOptions = function(statsOptions, defaultDB){
			var selectedOptions = {};
			statsOptions.forEach(function(p){ selectedOptions[p] = jquery.extend({}, defaultDB); /* cloning defaultDB */ });
			return selectedOptions;
		};
		
		this.createScenarioRequest = function(mixtureId, selectedOptions, selectedHypotesisProsecutor, selectedHypotesisDefender){
			var statsOptions = {}; 
				
			Object.keys(selectedOptions).forEach(function(p){
				var aux = selectedOptions[p];
				var profile = p.replace(postfixDView, postfixD).replace(postfixPView, postfixP);
				statsOptions[profile] = {'profile': profile, 'freqDb': aux.freqDB};
			});
			
			var req = {'mixProfile': mixtureId, 
					'prosecutorHypothesis': selectedHypotesisProsecutor.map(function(p){ return p.replace(postfixPView, postfixP); }), 
					'defenderHypothesis': selectedHypotesisDefender.map(function(p){ return p.replace(postfixDView, postfixD); }),
					'statsOptions': statsOptions};
			
			return req;
		};
		
		this.getDefaultDefenderHypotesis = function(hypotesesDefender, contributorsCount){
			return hypotesesDefender.filter(function(x){
				var re = new RegExp(postfixDView, 'g');
				var qq = x.join().match(re);
				return qq && qq.length === contributorsCount;
			})[0];
		};

		this.getDefaultProsecutorHypotesis = function(hypotesisProsecutor, paramsProfilesCount){
			return hypotesisProsecutor.filter(function(x){
				var re = new RegExp(postfixPView, 'g');
				var qq = x.join().match(re);
				return (paramsProfilesCount === 1 && qq.length === 1) || (!qq);
			})[0];
		};
		
		
	}
	return HypothesesService;
});
		