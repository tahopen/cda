/*!
 * Copyright 2002 - 2017 Webdetails, a Hitachi Vantara company. All rights reserved.
 *
 * This software was developed by Webdetails and is provided under the terms
 * of the Mozilla Public License, Version 2.0, or any later version. You may not use
 * this file except in compliance with the license. If you need a copy of the license,
 * please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. Please refer to
 * the license for the specific language governing your rights and limitations.
 */

package pt.webdetails.cda.connections.mondrian;

import java.util.List;
import java.util.Properties;

import org.dom4j.Element;
import org.apache.commons.lang.StringUtils;

public class JdbcConnectionInfo implements MondrianConnectionInfo {

  private String driver;
  private String url;
  private String user;
  private String pass;
  private String catalog;
  private String cube;
  private Properties properties;
  private String roleField;
  private String userField;
  private String passwordField;
  private String mondrianRole;

  public JdbcConnectionInfo( final Element connection ) {

    final String driver = (String) connection.selectObject( "string(./Driver)" );
    final String url = (String) connection.selectObject( "string(./Url)" );

    final String userName = (String) connection.selectObject( "string(./User)" );
    final String password = (String) connection.selectObject( "string(./Pass)" );
    final String role = (String) connection.selectObject( "string(./Role)" );

    final String roleFormula = (String) connection.selectObject( "string(./RoleField)" );
    final String userFormula = (String) connection.selectObject( "string(./UserField)" );
    final String passFormula = (String) connection.selectObject( "string(./PassField)" );

    if ( StringUtils.isEmpty( driver ) ) {
      throw new IllegalStateException( "A driver is mandatory" );
    }
    if ( StringUtils.isEmpty( url ) ) {
      throw new IllegalStateException( "A url is mandatory" );
    }

    setDriver( driver );
    setUrl( url );
    if ( userName != null ) {
      setUser( userName );
    }
    if ( password != null ) {
      setPass( password );
    }

    if ( StringUtils.isEmpty( role ) == false ) {
      setMondrianRole( role );
    }

    if ( StringUtils.isEmpty( userFormula ) == false ) {
      setUserField( userFormula );
    }
    if ( StringUtils.isEmpty( passFormula ) == false ) {
      setPasswordField( passFormula );
    }
    if ( StringUtils.isEmpty( roleFormula ) == false ) {
      setRoleField( roleFormula );
    }

    properties = new Properties();
    @SuppressWarnings( "unchecked" )
    final List<Element> list = connection.elements( "Property" );
    for ( final Element childElement : list ) {
      final String name = childElement.attributeValue( "name" );
      final String text = childElement.getText();
      properties.put( name, text );
    }

    setCatalog( (String) connection.selectObject( "string(./Catalog)" ) );
    setCube( (String) connection.selectObject( "string(./Cube)" ) );

  }

  public void setMondrianRole( final String mondrianRole ) {
    this.mondrianRole = mondrianRole;
  }

  public String getMondrianRole() {
    return mondrianRole;
  }

  public String getRoleField() {
    return roleField;
  }

  public void setRoleField( final String roleField ) {
    this.roleField = roleField;
  }

  public String getUserField() {
    return userField;
  }

  public void setUserField( final String userField ) {
    this.userField = userField;
  }

  public String getPasswordField() {
    return passwordField;
  }

  public void setPasswordField( final String passwordField ) {
    this.passwordField = passwordField;
  }

  public Properties getProperties() {
    return properties;
  }

  public String getDriver() {
    return driver;
  }

  public void setDriver( final String driver ) {
    this.driver = driver;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl( final String url ) {
    this.url = url;
  }

  public String getUser() {
    return user;
  }

  public void setUser( final String user ) {
    this.user = user;
  }

  public String getPass() {
    return pass;
  }

  public void setPass( final String pass ) {
    this.pass = pass;
  }

  public String getCatalog() {
    return catalog;
  }

  public void setCatalog( final String catalog ) {
    this.catalog = catalog;
  }

  public String getCube() {
    return cube;
  }

  public void setCube( final String cube ) {
    this.cube = cube;
  }

  public boolean equals( final Object o ) {
    if ( this == o ) {
      return true;
    }
    if ( !( o instanceof JdbcConnectionInfo ) ) {
      return false;
    }

    final JdbcConnectionInfo that = (JdbcConnectionInfo) o;

    if ( catalog != null ? !catalog.equals( that.catalog ) : that.catalog != null ) {
      return false;
    }
    if ( cube != null ? !cube.equals( that.cube ) : that.cube != null ) {
      return false;
    }
    if ( driver != null ? !driver.equals( that.driver ) : that.driver != null ) {
      return false;
    }
    if ( pass != null ? !pass.equals( that.pass ) : that.pass != null ) {
      return false;
    }
    if ( url != null ? !url.equals( that.url ) : that.url != null ) {
      return false;
    }
    if ( user != null ? !user.equals( that.user ) : that.user != null ) {
      return false;
    }

    return true;
  }

  public int hashCode() {
    int result = driver != null ? driver.hashCode() : 0;
    result = 31 * result + ( url != null ? url.hashCode() : 0 );
    result = 31 * result + ( user != null ? user.hashCode() : 0 );
    result = 31 * result + ( pass != null ? pass.hashCode() : 0 );
    result = 31 * result + ( catalog != null ? catalog.hashCode() : 0 );
    result = 31 * result + ( cube != null ? cube.hashCode() : 0 );
    return result;
  }
}
