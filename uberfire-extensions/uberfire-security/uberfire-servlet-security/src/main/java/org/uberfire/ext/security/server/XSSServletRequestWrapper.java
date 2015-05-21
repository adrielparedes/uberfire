package org.uberfire.ext.security.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Note: This implementation has been borrowed from Aerogear Security.
 */
public class XSSServletRequestWrapper extends HttpServletRequestWrapper {

    public XSSServletRequestWrapper( final HttpServletRequest request ) {
        super( request );
    }

    @Override
    public String[] getParameterValues( final String param ) {
        final String[] values = super.getParameterValues( param );

        for ( int i = 0; i < values.length; i++ ) {
            values[ i ] = StringEscapeUtils.escapeHtml4( values[ i ] );
        }

        return values;
    }

    @Override
    public String getParameter( final String param ) {
        return StringEscapeUtils.escapeHtml4( super.getParameter( param ) );
    }

}
