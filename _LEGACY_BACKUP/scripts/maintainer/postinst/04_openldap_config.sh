if [ $FISRT_INSTALL = true ] ; then

	echo "openldap config"

	dpkg-reconfigure slapd || true

	ldapadd -x -D "cn=admin,${GENIS_LDAP_DC}" -w $LDAP_ADMIN_PWD <<EOF
# Organizational Units

dn: ou=Users,$GENIS_LDAP_DC
objectClass: top
objectClass: organizationalUnit
ou: Users

dn: ou=Roles,$GENIS_LDAP_DC
objectClass: top
objectClass: organizationalUnit
ou: Roles

# LDAP Roles

dn: cn=admin,ou=Roles,$GENIS_LDAP_DC
objectClass: top
objectClass: organizationalRole
cn: admin
ou: Roles
street: DNA_PROFILE_CRUD
street: PROTOPROFILE_BULK_UPLOAD
street: PROFILE_DATA_CRUD
street: PEDIGREE_CRUD
street: BIO_MAT_CRUD
street: OPERATION_LOG_READ
street: LABORATORY_CRUD
street: MATCHES_MANAGER
street: ALLELIC_FREQ_DB_CRUD
street: PROTOPROFILE_BULK_ACCEPTANCE
street: ALLELIC_FREQ_DB_VIEW
street: ROLE_CRUD
street: USER_CRUD
street: CATEGORY_CRUD
street: PROFILE_DATA_SEARCH
street: GENETICIST_CRUD
street: SCENARIO_CRUD
street: LOCUS_CRUD
street: KIT_CRUD
description: Administrador


# Default User

dn:uid=$GENIS_DEFAULT_USR_CN,ou=Users,$GENIS_LDAP_DC
objectClass: person
objectClass: organizationalPerson
objectClass: inetOrgPerson
cn:$GENIS_DEFAULT_USR_CN
sn: Default instance user
givenName: Default instance user
mail:$GENIS_DEFAULT_USR_CN@$GENIS_ROOT_DOMAIN
telephoneNumber:00000000
employeeType: admin
street:active
title: false
o:genis-admin
userPassword:$GENIS_DEFAULT_USR_PWD_SHA
jpegPhoto::$GENIS_DEFAULT_USR_ENCRYPTED_TOTP

EOF

fi