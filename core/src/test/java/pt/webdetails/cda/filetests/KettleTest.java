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

package pt.webdetails.cda.filetests;

import javax.swing.table.TableModel;

import org.pentaho.reporting.engine.classic.core.util.TypedTableModel;

import pt.webdetails.cda.query.QueryOptions;
import pt.webdetails.cda.settings.CdaSettings;
import pt.webdetails.cda.test.util.TableModelChecker;

public class KettleTest extends CdaTestCase {

  public void testSampleKettle() throws Exception {

    final CdaSettings cdaSettings = parseSettingsFile( "sample-kettle.cda" );

    final QueryOptions queryOptions = new QueryOptions();
    queryOptions.setDataAccessId( "2" );
    queryOptions.addParameter( "myRadius", "10" );
    queryOptions.addParameter( "ZipCode", "90210" );

    TableModel result = doQuery( cdaSettings, queryOptions );
    TableModelChecker checker = new TableModelChecker( true, true );
    TypedTableModel expect =
      new TypedTableModel( new String[] { "radius", "zip" }, new Class<?>[] { Double.class, String.class } );
    expect.addRow( 10d, "90210" );
    checker.assertEquals( expect, result );
  }

  public void testKettleStringArray() throws Exception {
    final CdaSettings cdaSettings = parseSettingsFile( "sample-kettle-ParamArray.cda" );

    final QueryOptions queryOptions = new QueryOptions();
    queryOptions.setDataAccessId( "1" );

    queryOptions.setParameter( "countries", "Portugal;Germany" );
    queryOptions.setParameter( "Costumers", "307;369" );

    TableModel tm = doQuery( cdaSettings, queryOptions );
    assertEquals( 2, tm.getRowCount() );
    assertEquals( "307", tm.getValueAt( 0, 0 ).toString() );
    assertEquals( "Der Hund Imports", tm.getValueAt( 0, 1 ) );
  }

}
