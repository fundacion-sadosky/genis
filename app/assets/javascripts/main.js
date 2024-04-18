// `main.js` is the file that sbt-web will use as an entry point
(function(requirejs) {
'use strict';

// -- RequireJS config --
requirejs.config({
	// Packages = top-level folders; loads a contained file named 'main.js"
	packages : [ 'common', 'home', 'users', 'login', 'signup', 'geneticists', 'categories', 'profiledata', 'laboratories','stats', 'search', 'audit', 'profiles', 'matches', 'bulkupload', 'roles', 'pedigree', 'biomaterialtype', 'scenarios', 'matchesGroups', 'inbox', 'locus', 'kits', 'trace', 'pedigreeMatches', 'pedigreeMatchesGroups','superiorinstance','inferiorinstance','profileapproval','motives', 'reporting','mutations','profileExporter', 'profileExporterToLims'],
	shim : {
		'jsRoutes' : {
			deps : [],
			// it's not a RequireJS module, so we have to tell it what var
			// is returned
			exports : 'jsRoutes'
		},
		// Hopefully this all will not be necessary but can be fetched from
		// WebJars in the future
		'angular' : {
			deps : [ 'jquery' ],
			exports : 'angular'
		},
		'angular-file-upload' : {
			deps : [ 'angular' ],
			exports : 'angular'
		},
		'angular-hotkeys' : {
			deps : ['angular'],
			exports : 'angular'
		},
		'cryptojs-aes' : {
			deps : [ 'cryptojs-core' ],
			exports : 'CryptoJS'
		},
		'cryptojs-enc' : {
			deps : [ 'cryptojs-core' ],
			exports : 'CryptoJS'
		},
		'angular-route' : [ 'angular' ],
		'angular-cookies' : [ 'angular' ],
		'bootstrap' : [ 'jquery' ],
		'ng-bootstrap' : [ 'angular', 'bootstrap' ],
		'ng-select' : [ 'angular', 'bootstrap' ],
		'i18next' : [ 'jquery' ],
		'ng-i18next' : [ 'angular', 'i18next' ],
		'qrcodejs' : {exports : 'QRCode'},
        'jquery-ui': ['jquery'],
        'angular-ui-sortable': ['jquery-ui']
    },
	paths : {
        'requirejs' :  '../lib/requirejs/require' ,
        'jquery' :  '../lib/jquery/jquery' ,
        'angular' :  '../lib/angularjs/angular' ,
        'angular-route' :  '../lib/angularjs/angular-route' ,
        'angular-cookies' :  '../lib/angularjs/angular-cookies' ,
        'bootstrap' :  '../lib/bootstrap/js/bootstrap' ,
        'ng-bootstrap' :  '../lib/angular-ui-bootstrap/ui-bootstrap-tpls' ,
        'ng-select' :  '../lib/angular-ui-select/select' ,
        'angular-file-upload' : '../lib/angular-file-upload/ng-file-upload',
        'angular-hotkeys' : '../lib/angular-hotkeys/hotkeys.min',
        'cryptojs-core' :  '../lib/cryptojs/components/core-min',
        'cryptojs-enc' :  '../lib/cryptojs/components/enc-base64-min' ,
        'cryptojs-aes' :  '../lib/cryptojs/rollups/aes' ,
        'i18next' : '../lib/i18next/i18next',
        'ng-i18next' : '../lib/ng-i18next/ng-i18next',
        'sensitiveOper' : '/sensitiveOper',
        'appConf' : '/appConf',
        'jsRoutes' :  '/jsroutes' ,
        'qrcodejs' : '../lib/qrcodejs/qrcode',
        'd3': '../lib/d3js/d3',
        'dagre-d3': '../lib/dagre-d3/dagre-d3',
        'jquery-ui': '../lib/jquery-ui/jquery-ui',
        'angular-ui-sortable': '../lib/angular-ui-sortable/sortable',
        'lodash': '../lib/lodash/lodash'
    }
});


// Load the app. This is kept minimal so it doesn't need much updating.
require([ 'angular', 'angular-cookies', 'angular-route', 'jquery',
		'bootstrap', 'ng-bootstrap', 'ng-select', 'angular-hotkeys', 
		'angular-file-upload', 'ng-i18next', 'qrcodejs', 'dagre-d3',
		'jquery-ui', 'angular-ui-sortable', './app' ], function(angular) {
	
	angular.module('jm.i18next').config(['$i18nextProvider', function ($i18nextProvider) {
		$i18nextProvider.options = {
			lng: 'en',
			useCookie: false,
			useLocalStorage: false,
			fallbackLng: 'en',
			getAsync: false, 
			resGetPath: '/assets/locales/__lng__/__ns__.json',
			defaultLoadingValue: '' // ng-i18next option, *NOT* directly supported by i18next
		};
	}]);

	var ngApp = angular.bootstrap(document, ['ngRoute', 'ngCookies', 'jm.i18next', 'ui.bootstrap', 'ngFileUpload', 'ui.select', 'cfp.hotkeys','app'], { strictDi: true });
	ngApp.get('$rootScope').$broadcast('pdg.loaded');

});


})(requirejs);
