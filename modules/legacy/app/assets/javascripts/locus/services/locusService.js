define(['lodash', 'angular'],function(_,angular) {
    'use strict';

    function LocusService(playRoutes, $http) {

        this.add = function (locus) {
            return playRoutes.controllers.Locis.add().post(locus);
        };

        this.update = function (locus) {
            return playRoutes.controllers.Locis.update().put(locus);
        };

        this.listFull = function () {
            return playRoutes.controllers.Locis.listFull().get();
        };

        this.list = function () {
            return playRoutes.controllers.Locis.list().get();
        };
        
        this.delete = function (id) {
            return playRoutes.controllers.Locis.delete(id).delete();
        };

        this.getFluorophoreEnum = function() {
            return { YFP: 'Amarillo', RFP: 'Rojo', GFP: 'Verde', CFP: 'Cyan', BFP: 'Azul', VFP: 'Violeta', NFP: 'Negro'};
        };

        this.isOutOfLadder = function(l,allele){
            if(isNaN(allele) ||  _.isUndefined(l) || _.isUndefined(l.minAlleleValue) || _.isUndefined(l.maxAlleleValue)){
                return false;
            }
            return l.minAlleleValue>allele || l.maxAlleleValue<allele;
        };
        this.listRanges = function () {
            return playRoutes.controllers.Locis.ranges().get();
        };

        this.shouldShowMinAlelle = function(allele,alleleRange){
            if(_.isUndefined(alleleRange) || _.isUndefined(allele)){
                return false;
            }
            var isNumericAndLowerThanMin = !isNaN(allele)&& allele<alleleRange.min;
            var isMicroVariantAndLowerThanMin = false;
            var isOutOfLadderAndLowerThanMin = false;

            if(this.isMicroVariant(allele)){
                isMicroVariantAndLowerThanMin = this.getMicroVariantBase(allele)<alleleRange.min;
            }
            if(this.isOutOfLadderLower(allele)){
                isOutOfLadderAndLowerThanMin = this.getOutOfLadderBase(allele)<=alleleRange.min;
            }
            return isNumericAndLowerThanMin || isMicroVariantAndLowerThanMin || isOutOfLadderAndLowerThanMin;
        }.bind(this);

        this.shouldShowMaxAlelle = function(allele,alleleRange){
            if(_.isUndefined(alleleRange) || _.isUndefined(allele)){
                return false;
            }
            var isNumericAndGreatherThanMax = !isNaN(allele) && allele>alleleRange.max;
            var isMicroVariantGreatherThanMax = false;
            var isOutOfLadderGreatherThanMax = false;

            if(this.isMicroVariant(allele)){
                isMicroVariantGreatherThanMax = this.getMicroVariantBase(allele)>=alleleRange.max;
            }
            if(this.isOutOfLadderGreater(allele)){
                isOutOfLadderGreatherThanMax = this.getOutOfLadderBase(allele)>=alleleRange.max;
            }
            return isNumericAndGreatherThanMax || isMicroVariantGreatherThanMax || isOutOfLadderGreatherThanMax;
        }.bind(this);

        this.isMicroVariant = function(allele){
            return angular.isString(allele)&&_.endsWith(allele,'.X') && !isNaN(allele.substring(0,allele.length-2));
        }.bind(this);
        this.isOutOfLadderGreater = function(allele){
            return angular.isString(allele)&&_.endsWith(allele,'>') && !isNaN(allele.substring(0,allele.length-1));
        }.bind(this);
        this.isOutOfLadderLower = function(allele){
            return angular.isString(allele)&&_.endsWith(allele,'<') && !isNaN(allele.substring(0,allele.length-1));
        }.bind(this);
        this.getMicroVariantBase = function(allele){
            return allele.substring(0,allele.length-2);
        }.bind(this);
        this.getOutOfLadderBase = function(allele){
            return allele.substring(0,allele.length-1);
        }.bind(this);
        this.exportLocus = function () {
            return playRoutes.controllers.Locis.export().get();
        };
        this.importLocus = function(formData) {
            // Extract the URL from the Play routes object.
            var url = playRoutes.controllers.Locis.importLocus().url;

            // Use $http directly to ensure proper FormData handling.
            return $http.post(url, formData, {
                transformRequest: angular.identity,
                headers: { 'Content-Type': undefined }  // Let the browser set multipart/form-data with boundary.
            });
        };
    }
        
    return LocusService;

});