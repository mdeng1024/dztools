package com.jforex.dzjforex.brokerapi.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.dukascopy.api.system.IClient;
import com.jforex.dzjforex.brokerapi.BrokerTime;
import com.jforex.dzjforex.config.ZorroReturnValues;
import com.jforex.dzjforex.misc.MarketData;
import com.jforex.dzjforex.test.util.CommonUtilForTest;
import com.jforex.dzjforex.time.ServerTimeProvider;
import com.jforex.dzjforex.time.TimeConvert;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class BrokerTimeTest extends CommonUtilForTest {

    private BrokerTime brokerTime;

    @Mock
    private IClient clientMock;
    @Mock
    private ServerTimeProvider serverTimeProviderMock;
    @Mock
    private MarketData marketDataMock;
    private final double serverTimeParams[] = new double[1];
    private int returnCode;

    private static final long serverTime = 1234567L;

    @Before
    public void setUp() {
        when(serverTimeProviderMock.get()).thenReturn(serverTime);

        brokerTime = new BrokerTime(clientMock,
                                    serverTimeProviderMock,
                                    marketDataMock);
    }

    private void setClientConnectivity(final boolean isClientConnected) {
        when(clientMock.isConnected()).thenReturn(isClientConnected);
    }

    private void assertReturnCode(final int expectedReturnCode) {
        assertThat(returnCode, equalTo(expectedReturnCode));
    }

    private void assertServerTimeWasSetCorrect() {
        assertThat(serverTimeParams[0], equalTo(TimeConvert.getOLEDateFromMillis(serverTime)));
    }

    public class TestWhenClientIsDisconnected {

        @Before
        public void setUp() {
            setClientConnectivity(false);
            returnCode = brokerTime.get(serverTimeParams);
        }

        @Test
        public void returnCodeIndicatesNewLoginRequired() {
            assertReturnCode(ZorroReturnValues.CONNECTION_LOST_NEW_LOGIN_REQUIRED.getValue());
        }

        @Test
        public void noServerTimeWasSet() {
            assertThat(serverTimeParams[0], equalTo(0.0));
        }
    }

    public class TestWhenClientIsConnected {

        private void setMarketConnectivityAndStart(final boolean isMarketOffline) {
            when(marketDataMock.isMarketOffline(serverTime)).thenReturn(isMarketOffline);
            returnCode = brokerTime.get(serverTimeParams);
        }

        @Before
        public void setUp() {
            when(clientMock.isConnected()).thenReturn(true);
        }

        public class WhenMarketIsOpen {

            @Before
            public void setUp() {
                setMarketConnectivityAndStart(false);
            }

            @Test
            public void serverTimeWasSetCorrect() {
                assertServerTimeWasSetCorrect();
            }

            @Test
            public void returnCodeIsConnected() {
                assertReturnCode(ZorroReturnValues.CONNECTION_OK.getValue());
            }
        }

        public class WhenMarketIsOffline {

            @Before
            public void setUp() {
                setMarketConnectivityAndStart(true);
            }

            @Test
            public void returnCodeIsConnectedButMarketClosed() {
                assertReturnCode(ZorroReturnValues.CONNECTION_OK_BUT_MARKET_CLOSED.getValue());
            }

            @Test
            public void serverTimeWasSetCorrect() {
                assertServerTimeWasSetCorrect();
            }
        }
    }
}
