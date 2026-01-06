#!/bin/env bash

print_help() {
    cat << EOF
Usage: $0 {major|minor|patch|--help}

Arguments:
    major : Bump the major version
    minor : Bump the minor version
    patch : Bump the patch version
    exact : Bump to the exact version
    --help: Show this help message

This script reads the current version from a file named 'Version',
bumps the specified part of the version, writes the new version
back to the 'Version' file, and updates the version in the following files:

- ABOUT.md
- ABOUT_EN.md
- AUTHORS.md
- AUTHORS_EN.md
- build.sbt
- CODE_OF_CONDUCT.md
- CODE_OF_CONDUCT_EN.md
- README.md
- README_EN.md
- CHANGELOG.md
EOF
}

# Check if the script is being sourced
if [ "${BASH_SOURCE[0]}" != "${0}" ]; then
    echo "This script is being sourced, it should be executed directly."
    return
fi

# Print help if requested
if [[ $1 == "--help" ]]; then
    print_help
    exit 0
fi

# Argument check
#if [[ $# -ne 1 ]]; then
#    echo "Invalid number of arguments."
#    print_help
#    exit 1
#fi
#
check_version_file_exists() {
    if [ ! -f Version ]; then
        echo "VERSION file does not exist."
        exit 1
    fi
}

read_current_version() {
    # Read version from VERSION file
    version=$(cat Version)

    # Validate the version format
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "Invalid version format. Expected 'major.minor.patch' where major, \
            minor and patch are integers."
        exit 1
    fi

    # Read version parts
    IFS='.' read -ra version_parts <<< "$version"
    major=${version_parts[0]}
    minor=${version_parts[1]}
    patch=${version_parts[2]}
}

check_file_exists() {
    if [ ! -f $1 ]; then
        echo "$1 file does not exist."
        return 1
    fi
    return 0
}

patch_version_files(){
    echo "Version bumped from $version to $major.$minor.$patch"

    # Write new version into Version file
    echo "$major.$minor.$patch" > Version

    # Update the verion number in files

    # README.md and README_EN.md uses the
    # same regex
    query="(# GENis )[0-9]+\.[0-9]+\.[0-9]+"
    replacement="\1$major.$minor.$patch"
    check_file_exists README.md && \
        sed -i -r "s/$query/$replacement/" \
        README.md

    check_file_exists README_EN.md && \
        sed -i -r "s/$query/$replacement/" \
        README_EN.md

    # ABOUT.md, AUTHORS.md CODE_OF_CONDUCT.md uses the
    # same regex
    query="(^.*versión )[0-9]+\.[0-9]+\.[0-9]+"
    replacement="\1$major.$minor.$patch"
    check_file_exists ABOUT.md && \
        sed -i -r "s/$query/$replacement/" \
        ABOUT.md

    check_file_exists AUTHORS.md && \
        sed -i -r "s/$query/$replacement/" \
        AUTHORS.md

    check_file_exists CODE_OF_CONDUCT.md && \
        sed -i -r "s/$query/$replacement/" \
        CODE_OF_CONDUCT.md

    # ABOUT_EN.md, AUTHORS_EN.md and CODE_OF_CONDUCT_EN.md uses the
    # same regex
    query="(^.*version )[0-9]+\.[0-9]+\.[0-9]+"
    check_file_exists ABOUT_EN.md && \
        sed -i -r "s/$query/$replacement/" \
        ABOUT_EN.md

    check_file_exists AUTHORS_EN.md && \
        sed -i -r "s/$query/$replacement/" \
        AUTHORS_EN.md

    check_file_exists CODE_OF_CONDUCT_EN.md && \
        sed -i -r "s/$query/$replacement/" \
        CODE_OF_CONDUCT_EN.md

    # build.sbt uses its own regex
    query="(version := \")[0-9]+\.[0-9]+\.[0-9]+(\")"
    replacement="\1$major.$minor.$patch\2"
    check_file_exists build.sbt && \
        sed -i -r "s/$query/$replacement/" \
        build.sbt
  
    # CHANGELOG.md uses it own regex
    query="^# Changelog"
    msg="_Si está actualizando el sistema por favor lea:  [\`UPGRADING.md\`](https:\/\/github.com\/fundacion-sadosky\/genis\/blob\/main\/UPGRADING.md)._"
    current_date=$(date +"%Y-%m-%d")
    replacement="# Changelog\n\n\
## v[$major.$minor.$patch] - $current_date\n\n\
##msg##\n\n\
### Changed\n\n\
### Added\n\n\
### Fixed\n\n\
[v$major.$minor.$patch]: https:\/\/github.com\/fundacion-sadosky\/genis\/releases\/tag\/v$major.$minor.$patch"
    check_file_exists CHANGELOG.md && \
        sed -i -r "s/$query/$replacement/" CHANGELOG.md
        sed -i -r "/Si está actualizando/{N;d;}" CHANGELOG.md
        sed -i -r "s/##msg##/$msg/" CHANGELOG.md
    
    echo "Please Update the changes, additions and fixes in CHANGELOG.md"

}

# Bump version
case $1 in
    major)
        check_version_file_exists
        read_current_version
        major=$((major + 1))
        minor=0
        patch=0
        patch_version_files
        exit 0
        ;;
    minor)
        check_version_file_exists
        read_current_version
        minor=$((minor + 1))
        patch=0
        patch_version_files
        exit 0
        ;;
    patch)
        check_version_file_exists
        read_current_version
        patch=$((patch + 1))
        patch_version_files
        exit 0
        ;;
    exact)
        check_version_file_exists
        if [[ -z $2 ]]; then
            echo "Error: The 'exact' option requires a version number argument (e.g., 'exact 1.2.3')."
            exit 1
        fi
        IFS='.' read -r major minor patch <<< "$2"
        if [[ -z $major || -z $minor || -z $patch ]]; then
            echo "Error: Invalid version format. Please use 'major.minor.patch' (e.g., '1.2.3')."
            exit 1
        fi
        patch_version_files
        exit 0
        ;;
    *)
        echo "Invalid argument. Please choose 'major', 'minor', or 'patch'"
        print_help
        exit 1
        ;;
esac

