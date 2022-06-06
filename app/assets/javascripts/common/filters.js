/** Common filters. */
define([ 'angular','appConf' ], function(angular,appConf) {
	'use strict';

	var mod = angular.module('common.filters', []);
	/**
	 * Extracts a given property from the value it is applied to. {{{ (user |
	 * property:'name') }}}
	 */
	mod.filter('property', function(value, property) {
		if (angular.isObject(value)) {
			if (value.hasOwnProperty(property)) {
				return value[property];
			}
		}
	});

	mod.filter('range', function() {
		return function(input, total) {
			total = parseInt(total);
			for (var i = 0; i < total; i++) {
				input.push(i);
			}
			return input;
		};
	});
	
	mod.filter('translatematchstatus',function(){
		return function(input){
			var result = input;
			if(input === "PENDING"){
				result = "PENDIENTE";
			}else if(input === "DISCARDED"){
				result = "DESCARTADO";
			}else if(input === "DELETED"){
                result = "ELIMINADO";
            }else{
				result = "CONFIRMADO";
			}
			
			return result;
		};
	});
	
	// var alertMsgNoIntersection = 'N/A';
	var alertMsgNoFreqValue = 'Sin frecuencia';
	var alertMsgNotUsed = 'No usado';

	mod.filter('likelihoodratio', function() {
		return function(input, alleleMatching, statsResolved) {
			if (statsResolved){
				
				// if (!alleleMatching) {
				//return alertMsgNoIntersection;
				// }
				
				if (input === '0' || input === 0) {
					return input;
				}
				
				if (input === null) {
					return alertMsgNoFreqValue;
				}
				
				if (input === undefined) {
					return alertMsgNotUsed;
				}
                if(input.toString().includes("E") && !isNaN(input)){
                    input = parseFloat(input.toString());
				}
				if (!isNaN(input)) {
					//return (1.0/input).toPrecision(6);
					if (input.toString().includes("-") || input.toString().includes("+")  ){
						return input.toExponential(2);
					}

					var inputInt = parseInt(input);
					var decimalPartComplete = (input % 1).toString();
					var decimalPart;
					if(decimalPartComplete.length>=2) {
                        input = parseFloat(input.toString());

                        decimalPart = decimalPartComplete.substring(2);

                        var inputDecimal = parseInt(decimalPart);

                        if (inputInt > 0 && inputInt < 10) {
                            return input.toFixed(2);
                        }

                        if (inputInt === 0 && (parseInt(inputDecimal / 10) > 0)) {
                            return input.toFixed(2);
                        }

                        return input.toExponential(2);
                    }else{
                        input = parseFloat(input.toString());
                        return input.toExponential(2);
                    }
				}
				
				if (alleleMatching === 'Mismatch') {
					return 'Sin match';
				}
				
				return input;
			}
			
			return '';
		};
	});

	mod.filter('mt', ['profileHelper', function (profileHelper) {
		return function(allele, locus, type, analysisTypes) {
            if (type && analysisTypes[type].mitochondrial && !locus.endsWith("RANGE")) {
                var values = profileHelper.getMtValues(allele.toString());
                return profileHelper.formatMt(values);
            } else {
                return allele;
            }            
		};
	}]);

    mod.filter('mitochondrial', ['profileHelper', function (profileHelper) {
        return function(allele, locus) {
            if (!locus.endsWith("RANGE")) {
                var values = profileHelper.getMtValues(allele.toString());
                return profileHelper.formatMt(values);
            } else {
                return allele;
            }
        };
    }]);
	
	mod.filter('showcode', function(){
	
		return function(input){
			if (!input) {return;}
			var internalSc = input.internalSampleCode;
			var globalCode = input.globalCode;
			var lab = globalCode.split("-")[2];
			if(lab === appConf.labCode){
				return internalSc;
			}else{
				return globalCode;
			}
		};
		
	});
	
	mod.filter('showzero', function() {
		return function(input) {
			return input && input!=='0' ? input : '';
		};
	});
	
	mod.filter('freqdb', function() {
		return function(input) {
			return input!==-1 ? input : 'fmin';
		};
	});
	
	mod.filter('prittyLimitTo', function() {
		return function(input, limit, isFileName) {
			return (input && input.length > limit)? 
					input.substr(0, limit) + "..." + ((isFileName && input.lastIndexOf('.') > -1) ? input.substr(input.lastIndexOf('.')) : '') 
						: input;
		};
	});
	
	mod.filter('percentage', ['$filter', function ($filter) {
		return function (input, decimals) {
			if (input === undefined) {
                return undefined;
            }
			return $filter('number')(input * 100, decimals) + '%';
		};
	}]);

    mod.filter('likelihoodratioComp', function() {
        return function(input, alleleMatching, statsResolved) {
            if (statsResolved){

                if (input === '0' || input === 0) {
                    return input;
                }

                if (input === null) {
                    return alertMsgNoFreqValue;
                }

                if (input === undefined) {
                    return alertMsgNotUsed;
                }
                if((input.toString().includes("E") || input.toString().includes("e")) && !isNaN(input)){
                    input = parseFloat(input.toString());
                }
                if (!isNaN(input)) {
                    if (input.toString().includes("-") || input.toString().includes("+")  ){
                        return input.toExponential(6);
                    }

                    var inputInt = parseInt(input);
                    var decimalPartComplete = (input % 1).toString();
                    var decimalPart;
                    if(decimalPartComplete.length>=2) {
                        input = parseFloat(input.toString());

                        decimalPart = decimalPartComplete.substring(2);

                        var inputDecimal = parseInt(decimalPart);

                        if (inputInt > 0 && inputInt < 100000) {
                            return input.toFixed(6);
                        }

                        if (inputInt === 0 && (parseInt(inputDecimal / 100000) > 0)) {
                            return input.toFixed(6);
                        }

                        return input.toExponential(6);
                    }else{
                        input = parseFloat(input.toString());
                        return input.toExponential(6);
                    }
                }

                if (alleleMatching === 'Mismatch') {
                    return 'Sin match';
                }

                return input;
            }

            return '';
        };
    });
    mod.filter('likelihoodratioCompScenario', function() {
        return function(input, alleleMatching, statsResolved) {
            if (statsResolved){

                if (input === '0' || input === 0) {
                    return input;
                }

                if (input === null) {
                    return alertMsgNoFreqValue;
                }

                if (input === undefined) {
                    return alertMsgNotUsed;
                }
                if((input.toString().includes("E") || input.toString().includes("e"))&& !isNaN(input)){
                    input = parseFloat(input.toString());
                }
                if (!isNaN(input)) {
                    if (input.toString().includes("-") || input.toString().includes("+")  ){
                        return input.toExponential(2);
                    }

                    var inputInt = parseInt(input);
                    var decimalPartComplete = (input % 1).toString();
                    var decimalPart;
                    if(decimalPartComplete.length>=2) {
                        input = parseFloat(input.toString());

                        decimalPart = decimalPartComplete.substring(2);

                        var inputDecimal = parseInt(decimalPart);

                        if (inputInt > 0 && inputInt < 10) {
                            return input.toFixed(2);
                        }

                        if (inputInt === 0 && (parseInt(inputDecimal / 10) > 0)) {
                            return input.toFixed(2);
                        }

                        return input.toExponential(2);
                    }else{
                        input = parseFloat(input.toString());
                        return input.toExponential(2);
                    }
                }

                if (alleleMatching === 'Mismatch') {
                    return 'Sin match';
                }

                return input;
            }

            return '';
        };
    });
	return mod;
});
