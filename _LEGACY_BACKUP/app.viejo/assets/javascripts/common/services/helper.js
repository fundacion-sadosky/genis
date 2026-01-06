/** Common helpers */
define(['angular'], function(angular/*, $parse*/) {
  'use strict';

  var mod = angular.module('common.helper', []);
  
//  mod.service('helper', function() {
//    return {
//      sayHi: function() {
//        return 'hi';
//      }
//    };
//  });
  
  mod.service('helper', [ '$parse', function ( $parse ) {
	return {
			sayHi: function() {
				return 'hi';
			},
			
			groupBy: function (collection, property) {		
				var getterFn = $parse(property);
				var result = {};
				var prop;

				angular.forEach( collection, function( elm ) {
					prop = getterFn(elm); 

					if(!result[prop]) {
						result[prop] = [];
					}
					result[prop].push(elm);
				});
				return result;
			},
			
			notSorted: function(obj){
				if (!obj) {return [];}
				return Object.keys(obj);
			}
			
		};
  }]);
	
  return mod;
});
