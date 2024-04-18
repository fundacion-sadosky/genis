define(['angular', 'jquery', 'appConf','lodash'], function(angular, $, appConf,_) {
'use strict';

function ProfileHelper() {

  var reWildCard = new RegExp("^B:\\d+$|^\\*$");
  
  var intersect = function (a, b) {
    var new_a = angular.copy(a);
    var intersection = [];
    b.forEach(function(elem) {
      var index = new_a.indexOf(elem); 
    if (index !== -1) {
      intersection.push(elem);
      new_a.splice(index,1);
    }
    });
    return intersection;
    };

    var arraysEquals = function (a, b) {
        if (a === b) { return true; }
        if (a === null || b === null) { return false; }
        if (a.length !== b.length) { return false; }

        for (var i = 0; i < a.length; ++i) {
            if (a[i] !== b[i]) { return false; }
        }
        return true;
    };
    
  var getNonWcAlleles = function(alleles) {
    return alleles.filter(function(x){return !String(x).match(reWildCard);});
  };

  var getMtValues = function(item) {
    var isMongo = item.indexOf("@")>-1;
    var rePos = new RegExp("\\d{1,5}");
    var reIns = new RegExp("\\d{1,5}\\.[12]");
    var reBase = new RegExp("[ACGTDRYSWKMBHVN-]");
    var pos = (item.match(reIns))? item.match(reIns): item.match(rePos);
    var originalBase = appConf.mtRCRS.tabla[parseInt(pos)];
    var tipo = "SUS";
    var inputOriginalBase = null;
    var base = item.slice(-1);
    if(item.indexOf(".")>=0){
      tipo = "INS";
    } else if(item.endsWith("DEL") || item.charAt(0) === "-"){
      tipo = "DEL";
    }
    if(isMongo){
      base = item.match(reBase);
      inputOriginalBase = originalBase;
    }else{
      if(tipo === "DEL"){
        inputOriginalBase = item.charAt(0);
        base = inputOriginalBase;
      }
      if(tipo === "SUS"){
        inputOriginalBase = item.charAt(0);
      }
    }
    return {
      pos: pos,
      base: base,
      originalBase:originalBase,
      tipo:tipo,
      inputOriginalBase:inputOriginalBase
    };
  };

    var formatMt = function(values) {
      if(values.tipo === "INS"){
        return '-' + values.pos + '' + values.base ;
      }
      if(values.tipo === "SUS"){
        return values.originalBase + '' + values.pos + '' + values.base;
      }
      if(values.tipo === "DEL"){
        return values.originalBase + '' + values.pos + 'DEL';
      }
      return 'formatError';
    };
    var formatMongoMt = function(values) {
      if(values.tipo === "INS"){
        return values.base + '@' + values.pos ;
      }
      if(values.tipo === "SUS"){
        return values.base + '@' + values.pos ;
      }
      if(values.tipo === "DEL"){
        return '-@' + values.pos;
      }
      return 'formatError';
    };
  return {
    getRegExMap: function (){
      //var re122 = new RegExp("^B:\\d+$|^\\*$|^(\\d+(\\.\\d{1,2})?)$|^(\\d+(\\.\\d{1,2})?:\\d+)$");
      var re122 = new RegExp("^(\\d{1,2}(\\.[1-9]{1,1})?)$|^(\\d{1,2}(\\.X)?)$|^(\\d{1,2}([><])?)$");
      var rexy = new RegExp("^X$|^Y$|^x$|^y$");
      var remt = new RegExp("^-\\d{1,5}\\.[12][ACGTDRYSWKMBHVN]\\s{0,1}$|^[ACGTDRYSWKMBHVN]\\d{1,5}[ACGTDRYSWKMBHVN]\\s{0,1}$|^[ACGTDRYSWKMBHVN]\\d{1,5}DEL\\s{0,1}$");
      return {
        '1': re122, 
        '2': re122, 
        '3': re122, 
        '4': re122, 
        '5': re122, 
        '6': re122, 
        '7': re122, 
        '8': re122, 
        '9': re122, 
        '10': re122, 
        '11': re122, 
        '12': re122, 
        '13': re122, 
        '14': re122, 
        '15': re122, 
        '16': re122, 
        '17': re122, 
        '18': re122, 
        '19': re122, 
        '20': re122, 
        '21': re122, 
        '22': re122, 
        'X': re122, 
        'Y': re122, 
        'XY': rexy, 
        'MT': remt
      };
    },
    getMtGenotyfication: function(g){
      var g2 = {};
      $.each(g, function(locus, alleles) {
                g2[locus] = alleles.map(function(item) {
                    if (locus.endsWith("RANGE")) {
                        return item;
                    } else {
                        var mtValues = getMtValues(item);
                        return formatMongoMt(mtValues);
                    }
                });
                // .sort(function(a, b) {
                //     var posA = a.substr(a.indexOf('@') + 1);
                //     var posB = b.substr(b.indexOf('@') + 1);
                //     return posA - posB;
                // });
      });
      return g2;
    },
    clearEmptyLoci: function(genotypification) {
      $.each(genotypification, function(locus, genot) {
        if (genot.length > 0 && genot.filter(function(allele){return allele !== '';}).length === 0){
          genotypification[locus] = [];
        }
      });
    },
    verifyGenotypification: function(existingGenotypification, newGenotypification, lociList) {
      var isDblCheckNeeded = false;
      
      $.each(newGenotypification, function(locus, genotypification) {
                var genot = genotypification.filter(function(x) { return x !== ""; });

        if (genot.length < 1) { return true; } // continue
        
        if (existingGenotypification[locus]){
          var indx = lociList.indexOf(lociList.filter(function(x){return x.id === locus;})[0]);
          lociList[indx].errorMsg = '';
          
          // cast everything as string
          var existingGenStr = existingGenotypification[locus].map(function(x){return String(x);});
          
          var intersection = intersect(existingGenStr, genot);
          
          // For wildCards and homo
          if (!arraysEquals(intersection,existingGenStr) || existingGenStr.length !== genot.length){
            var existingGenWithoutWc = getNonWcAlleles(existingGenStr);
            var intWithoutWc = intersect(existingGenWithoutWc, genot);
            if (intWithoutWc.length === existingGenWithoutWc.length) {
              isDblCheckNeeded = true;
              return true; // OK => continue
            }
            lociList[indx].errorMsg = 'Error en la comparación con valores existentes en la genotipificación';
          }
        } else { // if the locus is not present in the existingGenotypification => a dbl ckeck is needed
          isDblCheckNeeded = true;
        }
      });
      return isDblCheckNeeded;
    },
    verifyDuplicates: function(genotypification, lociList, isReference) {
        $.each(genotypification, function(locus, alleles) {
            var indx = lociList.indexOf(lociList.filter(function(x){return x.id === locus.replace("_2_RANGE","_RANGE");})[0]);
            if(indx>-1){
                lociList[indx].errorMsg = "";

                var counts = {};
                alleles.forEach(function(x) { counts[x] = (counts[x] || 0)+1; });

                for (var allele in counts) {
                    if (counts.hasOwnProperty(allele)) {
                        if (isReference && counts[allele] > 2) {
                            lociList[indx].errorMsg = 'El alelo ' + allele + ' se repite más de dos veces';
                        }
                        if (!isReference && counts[allele] > 1) {
                            lociList[indx].errorMsg = 'No se pueden repetir alelos en evidencias';
                        }
                    }
                }
            }
        });
    },
    verifyMitochondrial: function(genotypification, mt) {
      console.log('apconf',appConf);
            // var ranges = {};
            // var keys = Object.keys(appConf).filter(function(k) { return k.endsWith("RANGE"); });
            // keys.forEach(function(hvr) {
            //     ranges[hvr] = genotypification.hasOwnProperty(hvr) ? genotypification[hvr] : appConf[hvr];
            // });
      var hvs = Object.keys(genotypification).sort().filter(function(x){return !x.endsWith("_RANGE");});
      var isValid = true;
      hvs.forEach(
        function(hv) {
          mt[hv].errorMsg = "";
          mt[hv+'_RANGE'].errorMsg = "";
          if (isValid) {
            var variations = genotypification[hv];
            if(variations!== undefined){
              variations = variations.filter(function(a){return a.length>0;});
            }
            // TODO: [#34] This check should be removed.
            //       It should be allowed to enter empty variations, which
            //       indicates that the range has no differences with the
            //       reference mitochondrial sequence.
            // if(variations.length === 0){
            //     mt[hv].errorMsg = 'Debe completar al menos una variación';
            //     isValid = false;
            //     return;
            // }
            var range = genotypification[hv+'_RANGE'];
            if(range!== undefined){
              range = range.filter(function(a){return a.length>0;});
            }
            if(range===undefined || range.length !== 2) {
                mt[hv+'_RANGE'].errorMsg = 'El rango debe tener dos valores';
                isValid = false;
            } else if (parseInt(range[1]) < parseInt(range[0]) ||
                (range[0] <1 || range[1] > 16569) ||
                range[1] >576 && range[1] < 16024 ) {
                mt[hv+'_RANGE'].errorMsg = 'El rango es inválido';
                isValid = false;
            } else {
              if (_.uniq(variations).length !== variations.length) {
                mt[hv].errorMsg = 'Hay variaciones repetidas';
                isValid = false;
              } else {
                var outOfRange = variations.filter(
                  function(a) {
                    var pos = parseInt(getMtValues(a).pos);
                    return pos < parseInt(range[0]) || pos > parseInt(range[1]);
                  }
                );
                if (outOfRange.length > 0) {
                  mt[hv].errorMsg = 'Las variaciones no están comprendidas en el rango (' + range[0] + ',' + range[1] + ')';
                  isValid = false;
                } else {
                  var invalidVariation = variations.filter(
                    function(a) {
                      var mtVals = getMtValues(a);
                      if (mtVals.tipo === "DEL" ) {
                        return mtVals.inputOriginalBase !== mtVals.originalBase;
                      }
                      if (mtVals.tipo==="SUS") {
                          return (mtVals.inputOriginalBase !== mtVals.originalBase) ||
                            _.isEmpty(mtVals.base) ||
                            mtVals.inputOriginalBase === mtVals.base;
                      }
                      if(mtVals.tipo==="INS"){
                          return _.isEmpty(mtVals.base);
                      }
                      return _.isUndefined(mtVals.originalBase);
                    }
                  );
                  if (invalidVariation.length > 0) {
                    mt[hv].errorMsg = 'Existen variaciones inválidas '+invalidVariation;
                    isValid = false;
                  }
                }
              }
            }
          }
        }
      );
    return isValid;
  },
  defaultMarkers: function() {
      var defaultValues = {};
      var keys = Object
        .keys(appConf)
        .filter(function(k) { return k.endsWith("RANGE"); });
      keys.forEach(
        function(k) {
          defaultValues[k] = appConf[k]
            .map(function(a) { return a.toString(); });
        }
      );
      return defaultValues;
  },
  convertMt: function(allele, locus) {
      if (!locus.endsWith("RANGE")) {
          var values = getMtValues(allele.toString());
          return formatMt(values);
      } else {
          return allele;
      }
  },
  getNonWcAlleles: getNonWcAlleles,
  getMtValues: getMtValues,
  formatMt: formatMt
  };
}
return ProfileHelper;
});