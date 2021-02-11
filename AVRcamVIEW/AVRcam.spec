%define name AVRcamVIEW
%define version 01.07
%define release 1
%define jar %{name}.jar
%define ant_build_file build.linux.xml
%define run_script AVRcamVIEW.sh

Summary: AVRcamVIEW GUI for controlling the AVRcam
Name: %{name}
Version: %{version}
Release: %{release}
Source: %{name}-%{version}.tar.gz
License: GPL
Group: AVRcam
BuildRoot: %{_builddir}/%{name}-buildroot
Prefix: %{_prefix}

%description
This will build and install the AVRcamVIEW GUI

%prep
%setup -q

%build
ant -f %{ant_build_file}

%install
mkdir -p $RPM_BUILD_ROOT/opt/%{name}
mkdir -p $RPM_BUILD_ROOT/opt/%{name}/bin
mkdir -p $RPM_BUILD_ROOT/opt/%{name}/lib
mv %{jar} $RPM_BUILD_ROOT/opt/%{name}/lib
mv jre $RPM_BUILD_ROOT/opt/%{name}
mv %{run_script} $RPM_BUILD_ROOT/opt/%{name}/bin

%clean
rm -rf $RPM_BUILD_ROOT

%files
%attr(0755,root,root) /opt/%{name}/bin/%{run_script}
%attr(0644,root,root) /opt/%{name}/lib/%{jar}
%attr(-,root,root) /opt/%{name}/jre
%attr(0755,root,root) /opt/%{name}/jre/bin/java

%postun
rm -rf /opt/%{name}

%changelog
